package gca.in.xap.tools.operationtool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigaspaces.grid.gsa.AgentProcessDetails;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import gca.in.xap.tools.operationtool.model.ComponentType;
import gca.in.xap.tools.operationtool.model.DumpReport;
import gca.in.xap.tools.operationtool.model.VirtualMachineDescription;
import gca.in.xap.tools.operationtool.predicates.container.IsEmptyContainerPredicate;
import gca.in.xap.tools.operationtool.service.deployer.ApplicationDeployer;
import gca.in.xap.tools.operationtool.service.deployer.ProcessingUnitDeployer;
import gca.in.xap.tools.operationtool.service.restartstrategy.DemoteThenRestartContainerItemVisitor;
import gca.in.xap.tools.operationtool.service.restartstrategy.RestartContainerItemVisitor;
import gca.in.xap.tools.operationtool.service.restartstrategy.RestartManagerItemVisitor;
import gca.in.xap.tools.operationtool.service.restartstrategy.ShutdownAgentItemVisitor;
import gca.in.xap.tools.operationtool.userinput.UserConfirmationService;
import gca.in.xap.tools.operationtool.util.collectionvisit.CollectionVisitingStrategy;
import gca.in.xap.tools.operationtool.util.collectionvisit.SequentialCollectionVisitingStrategy;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.Admin;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.application.config.ApplicationConfig;
import org.openspaces.admin.dump.DumpResult;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsa.GridServiceAgents;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.GridServiceContainers;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnits;
import org.openspaces.admin.pu.config.ProcessingUnitConfig;
import org.openspaces.admin.pu.config.UserDetailsConfig;
import org.openspaces.admin.pu.topology.ProcessingUnitConfigHolder;
import org.openspaces.admin.vm.VirtualMachine;
import org.openspaces.admin.vm.VirtualMachineDetails;
import org.openspaces.admin.vm.VirtualMachines;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

@Slf4j
public class XapService {

	private static void awaitTermination(final List<Future<?>> taskResults) {
		for (Future<?> taskResult : taskResults) {
			try {
				taskResult.get();
			} catch (InterruptedException e) {
				log.error("InterruptedException while waiting for task to complete", e);
			} catch (ExecutionException e) {
				log.error("ExecutionException while waiting for task to complete", e);
			}
		}
	}

	static void awaitDeployment(
			@NonNull final ApplicationConfig applicationConfig,
			@NonNull final Application dataApp,
			final long deploymentStartTime,
			@NonNull final Duration timeout,
			@NonNull XapService xapService
	) throws TimeoutException {
		long timeoutTime = deploymentStartTime + timeout.toMillis();

		final String applicationConfigName = applicationConfig.getName();
		log.info("Waiting for application {} to deploy ...", applicationConfigName);

		Set<String> deployedPuNames = new LinkedHashSet<>();

		final ProcessingUnits processingUnits = dataApp.getProcessingUnits();

		// get the pu names in the best order of deployment (regarding dependencies between them)
		final List<String> puNamesInOrderOfDeployment = ApplicationConfigHelper.getPuNamesInOrderOfDeployment(applicationConfig);

		for (String puName : puNamesInOrderOfDeployment) {
			ProcessingUnit pu = processingUnits.getProcessingUnit(puName);
			awaitDeployment(pu, deploymentStartTime, timeout, timeoutTime, xapService);
			deployedPuNames.add(puName);
		}

		long appDeploymentEndTime = System.currentTimeMillis();
		long appDeploymentDuration = appDeploymentEndTime - deploymentStartTime;

		log.info("Deployed PUs : {}", deployedPuNames);
		log.info("Application deployed in : {} ms", appDeploymentDuration);
	}

	static void awaitDeployment(@NonNull ProcessingUnit pu, long deploymentStartTime, @NonNull Duration timeout, long expectedMaximumEndDate, @NonNull XapService xapService) throws TimeoutException {
		String puName = pu.getName();
		final int plannedNumberOfInstances = pu.getPlannedNumberOfInstances();

		final long candidatesContainersCount = Arrays
				.stream(xapService.findContainers())
				.filter(gsc -> pu.getRequiredContainerZones().isSatisfiedBy(gsc.getExactZones()))
				.filter(gsc -> !pu.isRequiresIsolation() || gsc.getProcessingUnitInstances().length == 0)
				.count();

		log.info("Waiting for PU {} to deploy {} instances ... (found {} GSCs matching PU constraints)", puName, plannedNumberOfInstances, candidatesContainersCount);

		long remainingDelayUntilTimeout = expectedMaximumEndDate - System.currentTimeMillis();
		if (remainingDelayUntilTimeout < 0L) {
			throw new TimeoutException("Application deployment timed out after " + timeout);
		}
		boolean finished = false;
		// print a message every 5 seconds, in order to show progress of deployment
		while (!finished && remainingDelayUntilTimeout > 0L) {
			long waitDuration = Math.min(remainingDelayUntilTimeout, 5000);
			finished = pu.waitFor(plannedNumberOfInstances, waitDuration, TimeUnit.MILLISECONDS);
			remainingDelayUntilTimeout = expectedMaximumEndDate - System.currentTimeMillis();
			final int currentInstancesCount = pu.getInstances().length;
			log.warn("ProcessingUnit {} now has {} running instances", puName, currentInstancesCount, timeout);
		}
		if (!finished) {
			final int currentInstancesCount = pu.getInstances().length;
			log.warn("ProcessingUnit {} has {} running instances after timeout of {} has been reached", puName, currentInstancesCount, timeout);
			throw new TimeoutException("Application deployment timed out after " + timeout);
		}

		final long deploymentEndTime = System.currentTimeMillis();
		final long deploymentDuration = deploymentEndTime - deploymentStartTime;

		final int currentInstancesCount = pu.getInstances().length;
		log.info("PU {} deployed successfully after {} ms, now has {} running instances", puName, deploymentDuration, currentInstancesCount);

	}

	private static long durationSince(long time) {
		return System.currentTimeMillis() - time;
	}

	private final DateTimeFormatter dumpsFileNamesDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	@Setter
	private Admin admin;

	/**
	 * the timeout of the operations
	 */
	@Setter
	private Duration operationTimeout = Duration.of(1, ChronoUnit.MINUTES);

	/**
	 * the timeout of undeployments
	 */
	@Setter
	private Duration undeployProcessingUnitTimeout = Duration.ofMinutes(10);

	@Setter
	private UserDetailsConfig userDetails;

	@Setter
	private ExecutorService executorService;

	@Setter
	private UserConfirmationService userConfirmationService;

	@Setter
	private IdExtractor idExtractor;

	@Setter
	private PuRelocateService puRelocateService;

	@Setter
	private ProcessingUnitDeployer processingUnitDeployer;

	@Setter
	private ApplicationDeployer applicationDeployer;

	@Setter
	private DemoteThenRestartContainerItemVisitor demoteThenRestartContainerItemVisitor;

	@Setter
	private RestartContainerItemVisitor restartContainerItemVisitor;

	@Setter
	private RestartManagerItemVisitor restartManagerItemVisitor;

	@Setter
	private ShutdownAgentItemVisitor shutdownAgentItemVisitor;

	private final ObjectMapper objectMapper = new ObjectMapperFactory().createObjectMapper();

	public GridServiceContainer[] findContainers() {
		GridServiceContainers gridServiceContainers = admin.getGridServiceContainers();
		GridServiceContainer[] containers = gridServiceContainers.getContainers();
		// we want the GSCs to be sorted by Id, for readability and reproducibility
		Arrays.sort(containers, comparing(GridServiceContainer::getId));
		return containers;
	}

	public GridServiceManager[] findManagers() {
		GridServiceManagers gridServiceManagers = admin.getGridServiceManagers();
		GridServiceManager[] managers = gridServiceManagers.getManagers();
		// we want the GSMs to be sorted by Id, for readability and reproducibility
		Arrays.sort(managers, comparing(gsm -> gsm.getMachine().getHostName()));
		return managers;
	}

	public GridServiceAgent[] findAgents() {
		GridServiceAgents gridServiceAgents = admin.getGridServiceAgents();
		GridServiceAgent[] agents = gridServiceAgents.getAgents();
		// we want the GSAs to be sorted by Id, for readability and reproducibility
		Arrays.sort(agents, comparing(gsm -> gsm.getMachine().getHostName()));
		return agents;
	}

	public List<String> findManagersHostnames() {
		final GridServiceManager[] managers = findManagers();
		return Arrays.stream(managers).map(gsm -> gsm.getMachine().getHostName()).collect(Collectors.toList());
	}

	public Machine[] findAllMachines() {
		Machine[] machines = admin.getMachines().getMachines();
		Arrays.sort(machines, comparing(Machine::getHostName));
		return machines;
	}

	public ProcessingUnit findProcessingUnitByName(String processingUnitName) {
		ProcessingUnit processingUnit = admin.getProcessingUnits().getProcessingUnit(processingUnitName);
		return processingUnit;
	}

	public List<String> findAllProcessingUnitsNames() {
		ProcessingUnit[] processingUnits = admin.getProcessingUnits().getProcessingUnits();
		List<String> result = Arrays.stream(processingUnits).map(processingUnit -> processingUnit.getName()).collect(Collectors.toList());
		return result;
	}

	public void printReportOnContainersAndProcessingUnits() {
		printReportOnContainersAndProcessingUnits(gsc -> true);
	}

	public void printReportOnContainersAndProcessingUnits(Predicate<GridServiceContainer> predicate) {
		GridServiceContainer[] containers = findContainers();
		containers = Arrays.stream(containers).filter(predicate).toArray(GridServiceContainer[]::new);
		final int gscCount = containers.length;
		final Collection<String> containersIds = idExtractor.extractIds(containers);

		// regroup GSCs per hostname
		// create a MultiMap that is sorted on keys
		// key is the hostname
		// values are GSC
		final ListMultimap<String, GridServiceContainer> containersPerMachine = MultimapBuilder.treeKeys().linkedListValues().build();
		for (GridServiceContainer gsc : containers) {
			Machine currentGscMachine = gsc.getMachine();
			String hostName = currentGscMachine.getHostName();
			containersPerMachine.put(hostName, gsc);
		}

		log.info("Found {} matching running GSC instances : {}", gscCount, containersIds);
		for (Map.Entry<String, Collection<GridServiceContainer>> entry : containersPerMachine.asMap().entrySet()) {
			final String hostname = entry.getKey();
			final List<GridServiceContainer> containersForHost = (List) entry.getValue();
			final Set<String> commonZones = findCommonZones(containersForHost);
			Collections.sort(containersForHost, createGSCComparator(commonZones));
			log.info("On machine {} : {} : {} GSC instances", hostname, commonZones, containersForHost.size());
			for (GridServiceContainer gsc : containersForHost) {
				String gscId = gsc.getId();
				Set<String> specificZones = findSpecificZones(gsc, commonZones);
				ProcessingUnitInstance[] puInstances = gsc.getProcessingUnitInstances();
				final int puCount = puInstances.length;
				final Collection<String> puNames = idExtractor.extractProcessingUnitsNamesAndDescription(puInstances);
				log.info("GSC {} {} is running {} Processing Units : {}", String.format("%1$-15s", gscId), specificZones, puCount, puNames);
			}
		}

	}

	private Comparator<GridServiceContainer> createGSCComparator(final Set<String> commonZones) {
		Comparator<GridServiceContainer> result = Comparator
				.comparing(gsc -> findSpecificZones(gsc, commonZones).toString());
		result = result.thenComparing(gsc -> gsc.getVirtualMachine().getDetails().getPid());
		return result;
	}

	private Set<String> findCommonZones(Collection<GridServiceContainer> containers) {
		Set<String> result = new TreeSet<>();
		// first we put all the zones into the Set
		for (GridServiceContainer gsc : containers) {
			result.addAll(gsc.getExactZones().getZones());
		}
		// then we keep only the zones that are set on every GSC
		for (GridServiceContainer gsc : containers) {
			result.retainAll(gsc.getExactZones().getZones());
		}
		return result;
	}

	private Set<String> findSpecificZones(GridServiceContainer gsc, Set<String> commonZones) {
		Set<String> result = new TreeSet<>(gsc.getExactZones().getZones());
		result.removeAll(commonZones);
		return result;
	}

	public void printReportOnAgents() {
		final GridServiceAgent[] agents = findAgents();
		final int gsaCount = agents.length;
		final Collection<String> agentsIds = idExtractor.extractIds(agents);
		log.info("Found {} running GSA instances : {}", gsaCount, agentsIds);
		for (GridServiceAgent gsa : agents) {
			String hostName = gsa.getMachine().getHostName();
			Map<String, Integer> requiredGlobalInstances = gsa.getProcessesDetails().getRequiredGlobalInstances();
			AgentProcessDetails[] processDetailsList = gsa.getProcessesDetails().getProcessDetails();
			log.info("GSA {} : requiredGlobalInstances : {}, processes count = {}", hostName, requiredGlobalInstances, processDetailsList.length);
			for (AgentProcessDetails agentProcessDetails : processDetailsList) {
				log.info("InstantiationMode : {}, ServiceType : {}, ProcessId : {}, AgentId : {}",
						agentProcessDetails.getInstantiationMode(),
						agentProcessDetails.getServiceType(),
						agentProcessDetails.getProcessId(),
						agentProcessDetails.getAgentId());
			}
		}
	}

	public void printReportOnManagers() {
		final GridServiceManager[] managers = findManagers();
		final int gsmCount = managers.length;
		final Collection<String> managersIds = idExtractor.extractIds(managers);
		log.info("Found {} running GSM instances : {}", gsmCount, managersIds);
	}

	public void printReportOnVirtualMachines() {
		VirtualMachines virtualMachines = admin.getVirtualMachines();
		final int jvmCount = virtualMachines.getSize();
		log.info("Found {} JVMs", jvmCount);

		final List<VirtualMachineDescription> virtualMachineDescriptions = new ArrayList<>();


		for (VirtualMachine jvm : virtualMachines.getVirtualMachines()) {
			final VirtualMachineDetails details = jvm.getDetails();
			//
			VirtualMachineDescription vmDescription = new VirtualMachineDescription();
			vmDescription.setUid(jvm.getUid());
			vmDescription.setComponentType(guessComponentType(jvm));
			vmDescription.setUptime(Duration.ofMillis(jvm.getStatistics().getUptime()));
			vmDescription.setHostName(jvm.getMachine().getHostName());
			if (details != null) {
				final String jvmDescription = details.getVmVendor() + " : " + details.getVmName() + " : " + details.getVmVersion();
				vmDescription.setJvmDescription(jvmDescription);
				vmDescription.setPid(details.getPid());
				vmDescription.setHeapSizeInMBInit(Math.round(details.getMemoryHeapInitInMB()));
				vmDescription.setHeapSizeInMBMax(Math.round(details.getMemoryHeapMaxInMB()));
				vmDescription.setEnvironmentVariables(details.getEnvironmentVariables());
				vmDescription.setSystemProperties(details.getSystemProperties());
			}
			virtualMachineDescriptions.add(vmDescription);
		}

		virtualMachineDescriptions.sort(new VirtualMachineDescriptionComparator());

		for (VirtualMachineDescription jvm : virtualMachineDescriptions) {
			log.info("{} : {} : running on {} for {} : Heap [{} MB, {} MB] : {}",
					jvm.getComponentType(),
					String.format("%5d", jvm.getPid()),
					//jvm.getUid().substring(0, 7) + "...",
					jvm.getHostName(),
					padRight(jvm.getUptime(), 17),
					padLeft(jvm.getHeapSizeInMBInit(), 5),
					padLeft(jvm.getHeapSizeInMBMax(), 5),
					jvm.getJvmDescription());
			//Map<String, String> environmentVariables = jvm.getEnvironmentVariables();
			//String envVariableXapGscOptions = environmentVariables.get("XAP_GSC_OPTIONS");
			//log.info("envVariableXapGscOptions = {}", envVariableXapGscOptions);
		}
	}

	public static String padLeft(Object value, int length) {
		return String.format("%" + length + "s", value);
	}

	public static String padRight(Object value, int length) {
		return String.format("%-" + length + "s", value);
	}

	@NonNull
	public ComponentType guessComponentType(VirtualMachine jvm) {
		GridServiceContainer gridServiceContainer = jvm.getGridServiceContainer();
		if (gridServiceContainer != null) {
			return ComponentType.GSC;
		}
		GridServiceManager gridServiceManager = jvm.getGridServiceManager();
		if (gridServiceManager != null) {
			return ComponentType.GSM;
		}
		GridServiceAgent gridServiceAgent = jvm.getGridServiceAgent();
		if (gridServiceAgent != null) {
			return ComponentType.GSA;
		}
		return ComponentType.UNKNOWN;
	}

	/**
	 * you may want to restart containers after a PU has been undeployed, in order to make sure no unreleased resources remains.
	 */
	public void restartEmptyContainers() {
		log.warn("Will restart all empty GSC instances ... (GSC with no PU running)");
		restartContainers(new IsEmptyContainerPredicate(), new SequentialCollectionVisitingStrategy<>(Duration.ZERO), false);
	}

	public void restartContainers(
			@NonNull Predicate<GridServiceContainer> predicate,
			@NonNull CollectionVisitingStrategy<GridServiceContainer> collectionVisitingStrategy,
			boolean demoteFirst
	) {
		GridServiceContainer[] containers = findContainers();
		containers = Arrays.stream(containers).filter(predicate).toArray(GridServiceContainer[]::new);
		final int gscCount = containers.length;
		final Collection<String> containersIds = idExtractor.extractIds(containers);
		log.info("Found {} matching GSC instances : {}", gscCount, containersIds);

		log.warn("Will restart {} GSC instances : {}", gscCount, containersIds);
		userConfirmationService.askConfirmationAndWait();

		final CollectionVisitingStrategy.ItemVisitor itemVisitor;
		if (demoteFirst) {
			itemVisitor = this.demoteThenRestartContainerItemVisitor;
		} else {
			itemVisitor = restartContainerItemVisitor;
		}
		collectionVisitingStrategy.perform(containers, itemVisitor);

		log.info("Triggered restart of GSC instances : {}", containersIds);
	}

	public void restartManagers(@NonNull Predicate<GridServiceManager> predicate, @NonNull CollectionVisitingStrategy<GridServiceManager> collectionVisitingStrategy) {
		GridServiceManager[] managers = findManagers();
		managers = Arrays.stream(managers).filter(predicate).toArray(GridServiceManager[]::new);
		final int gsmCount = managers.length;
		final Collection<String> managersIds = idExtractor.extractIds(managers);
		log.info("Found {} matching GSM instances : {}", gsmCount, managersIds);

		log.warn("Will restart {] GSM instances : {}", gsmCount, managersIds);
		userConfirmationService.askConfirmationAndWait();
		collectionVisitingStrategy.perform(managers, restartManagerItemVisitor);
		log.info("Triggered restart of GSM instances : {}", managersIds);
	}

	public void shutdownAgents(@NonNull Predicate<GridServiceAgent> predicate, @NonNull CollectionVisitingStrategy<GridServiceAgent> collectionVisitingStrategy) {
		GridServiceAgent[] agents = admin.getGridServiceAgents().getAgents();
		agents = Arrays.stream(agents).filter(predicate).toArray(GridServiceAgent[]::new);
		final int gsaCount = agents.length;
		final Collection<String> agentIds = idExtractor.extractIds(agents);
		log.info("Found {} matching GSA instances : {}", gsaCount, agentIds);

		log.warn("Will shutdown {} GSA instances : {}", gsaCount, agentIds);
		userConfirmationService.askConfirmationAndWait();
		collectionVisitingStrategy.perform(agents, shutdownAgentItemVisitor);
		log.info("Triggered shutdown of GSA instances : {}", agentIds);
	}

	public void triggerGarbageCollectorOnContainers(@NonNull Predicate<GridServiceContainer> predicate, @NonNull CollectionVisitingStrategy<GridServiceContainer> collectionVisitingStrategy) {
		GridServiceContainer[] containers = findContainers();
		containers = Arrays.stream(containers).filter(predicate).toArray(GridServiceContainer[]::new);
		final int gscCount = containers.length;
		final Collection<String> containersIds = idExtractor.extractIds(containers);
		log.info("Found {} matching GSC instances : {}", gscCount, containersIds);

		log.warn("Will trigger Garbage Collector on {} GSC instances : {}", gscCount, containersIds);
		userConfirmationService.askConfirmationAndWait();
		collectionVisitingStrategy.perform(containers, gsc -> {
			final String gscId = gsc.getId();
			try {
				log.info("Triggering GC on GSC {} ...", gscId);
				gsc.getVirtualMachine().runGc();
			} catch (RuntimeException e) {
				log.error("Failure while triggering Garbage Collector on GSC {}", gscId, e);
			}
		});
		log.info("Triggered GC on GSC instances : {}", containersIds);
	}

	public void generateHeapDumpOnEachContainers(@NonNull Predicate<GridServiceContainer> predicate, @NonNull CollectionVisitingStrategy<GridServiceContainer> collectionVisitingStrategy) throws IOException {
		String[] dumpTypes = {"heap"};
		final File outputDirectory = new File("dumps/heap");
		generateDumpOnEachGsc(predicate, collectionVisitingStrategy, outputDirectory, dumpTypes);
	}

	public void generateThreadDumpOnContainers(@NonNull Predicate<GridServiceContainer> predicate, @NonNull CollectionVisitingStrategy<GridServiceContainer> collectionVisitingStrategy) throws IOException {
		String[] dumpTypes = {"thread"};
		final File outputDirectory = new File("dumps/thread");
		generateDumpOnEachGsc(predicate, collectionVisitingStrategy, outputDirectory, dumpTypes);
	}

	private void generateDumpOnEachGsc(@NonNull Predicate<GridServiceContainer> predicate, @NonNull CollectionVisitingStrategy<GridServiceContainer> collectionVisitingStrategy, final File outputDirectory, final String[] dumpTypes) throws IOException {
		GridServiceContainer[] containers = findContainers();
		containers = Arrays.stream(containers).filter(predicate).toArray(GridServiceContainer[]::new);
		final int gscCount = containers.length;
		final Collection<String> containersIds = idExtractor.extractIds(containers);
		log.info("Found {} matching GSC instances : {}", gscCount, containersIds);

		boolean outputDirectoryCreated = outputDirectory.mkdirs();
		log.debug("outputDirectoryCreated = {]", outputDirectoryCreated);
		if (!outputDirectory.canWrite()) {
			throw new IOException("Cannot write to directory " + outputDirectory + " (" + outputDirectory.getAbsolutePath() + "). Please execute the command from a working directory where you have write access.");
		}

		log.warn("Will trigger Dump on {} GSC instances : {}", gscCount, containersIds);
		userConfirmationService.askConfirmationAndWait();
		collectionVisitingStrategy.perform(containers, gsc -> {
			final String gscId = gsc.getId();
			try {
				generateDump(gsc, outputDirectory, dumpTypes);
			} catch (RuntimeException | IOException e) {
				log.error("Failure while generating a Heap Dump on GSC {}", gscId, e);
			}
		});
		log.info("Triggered Dump on GSC instances : {}", containersIds);
	}

	private void generateDump(@NonNull GridServiceContainer gsc, @NonNull final File outputDirectory, String[] dumpTypes) throws IOException {
		final String gscId = gsc.getId();

		final Machine machine = gsc.getMachine();

		long pid = gsc.getVirtualMachine().getDetails().getPid();

		ProcessingUnitInstance[] processingUnitInstances = gsc.getProcessingUnitInstances();
		Collection<String> processingUnitsNames = idExtractor.extractProcessingUnitsNames(processingUnitInstances);

		final ZonedDateTime time = ZonedDateTime.now();
		final String dumpFileName = "dump-" + gscId + "-" + time.format(dumpsFileNamesDateTimeFormatter) + ".zip";
		final String reportFileName = "dump-" + gscId + "-" + time.format(dumpsFileNamesDateTimeFormatter) + ".json";
		final File dumpFile = new File(outputDirectory, dumpFileName);
		final File reportFile = new File(outputDirectory, reportFileName);
		//

		final List<String> dumpsTypesList = Arrays.asList(dumpTypes);

		DumpReport dumpReport = new DumpReport();
		dumpReport.setDumpsTypes(dumpsTypesList);
		dumpReport.setGscId(gscId);
		dumpReport.setPid(pid);
		dumpReport.setStartTime(time);
		dumpReport.setDumpFileName(dumpFileName);
		dumpReport.setProcessingUnitsNames(new ArrayList<>(processingUnitsNames));
		dumpReport.setHostName(machine.getHostName());
		dumpReport.setHostAddress(machine.getHostAddress());
		objectMapper.writeValue(reportFile, dumpReport);

		//
		log.info("Asking GSC {} for a dump of {} ...", gscId, dumpsTypesList);
		final DumpResult dumpResult = gsc.generateDump("Generating a dump with XAP operation tool for " + dumpsTypesList, null, dumpTypes);
		log.info("Downloading dump from gsc {} to file {} ...", gscId, dumpFile.getAbsolutePath());
		dumpResult.download(dumpFile, null);
		log.info("Wrote file {} : size = {} bytes", dumpFile.getAbsolutePath(), dumpFile.length());
	}


	public void deployWhole(ApplicationConfig applicationConfig, Duration timeout) throws TimeoutException {
		log.info("Attempting deployment of application '{}' composed of : {} with a timeout of {}",
				applicationConfig.getName(),
				ApplicationConfigHelper.getPuNamesInOrderOfDeployment(applicationConfig),
				timeout
		);

		long deployRequestStartTime = System.currentTimeMillis();
		Application dataApp = applicationDeployer.deploy(applicationConfig, timeout.toMillis(), TimeUnit.MILLISECONDS);
		long deployRequestEndTime = System.currentTimeMillis();
		long deployRequestDuration = deployRequestEndTime - deployRequestStartTime;
		log.info("Requested deployment of application : duration = {} ms", deployRequestDuration);

		if (dataApp == null) {
			throw new DeploymentRequestException("Deployment request failed, GridServiceManagers returned null");
		}

		long deploymentStartTime = deployRequestEndTime;
		awaitDeployment(applicationConfig, dataApp, deploymentStartTime, operationTimeout, this);
	}

	public void deployProcessingUnits(
			ApplicationConfig applicationConfig,
			Predicate<String> processingUnitsPredicate,
			Duration timeout,
			boolean restartEmptyContainers
	) throws TimeoutException {
		log.info("Attempting deployment of application '{}' composed of : {} with a timeout of {}",
				applicationConfig.getName(),
				ApplicationConfigHelper.getPuNamesInOrderOfDeployment(applicationConfig),
				timeout
		);

		final long deploymentStartTime = System.currentTimeMillis();
		final long expectedMaximumEndDate = deploymentStartTime + timeout.toMillis();

		for (ProcessingUnitConfigHolder pu : applicationConfig.getProcessingUnits()) {
			final String puName = pu.getName();
			if (!processingUnitsPredicate.test(puName)) {
				log.info("Skipping Processing Unit {} as requested by user", puName);
			} else {
				doDeployProcessingUnit(pu, puName, timeout, expectedMaximumEndDate);
			}
		}

		long deployRequestEndTime = System.currentTimeMillis();
		long appDeploymentDuration = deployRequestEndTime - deploymentStartTime;

		log.info("Application deployed in: {} ms", appDeploymentDuration);
	}

	private void doDeployProcessingUnit(
			final ProcessingUnitConfigHolder pu,
			final String puName,
			final Duration timeout,
			final long expectedMaximumEndDate
	) throws TimeoutException {
		final ProcessingUnitConfig processingUnitConfig = pu.toProcessingUnitConfig();
		log.debug("puName = {}, processingUnitConfig = {}", puName, processingUnitConfig);

		undeployPu(puName, undeployProcessingUnitTimeout);

		log.info("Deploying pu {} ...", puName);
		long puDeploymentStartTime = System.currentTimeMillis();

		ProcessingUnit processingUnit = processingUnitDeployer.deploy(puName, processingUnitConfig);
		awaitDeployment(processingUnit, puDeploymentStartTime, timeout, expectedMaximumEndDate, this);
	}

	private void undeployPu(String puName, Duration timeout) {
		doWithProcessingUnit(puName, Duration.of(10, ChronoUnit.SECONDS), existingProcessingUnit -> {
			final int instancesCount = existingProcessingUnit.getInstances().length;
			log.info("Undeploying pu {} ... ({} instances are running on GSCs {})", puName, instancesCount, idExtractor.extractContainerIds(existingProcessingUnit));
			long startTime = System.currentTimeMillis();
			boolean undeployedSuccessful = existingProcessingUnit.undeployAndWait(timeout.toMillis(), TimeUnit.MILLISECONDS);
			long endTime = System.currentTimeMillis();
			long duration = endTime - startTime;
			if (undeployedSuccessful) {
				log.info("Undeployed pu {} in {} ms", puName, duration);
			} else {
				log.warn("Timeout waiting for pu {} to undeploy after {} ms", puName, duration);
			}
		}, s -> {
			log.info("ProcessingUnit " + puName + " is not already deployed");
		});
	}

	public void undeployApplication(String applicationName) {
		log.info("Launch undeploy of {}, operationTimeout = {}", applicationName, operationTimeout);
		doWithApplication(
				applicationName,
				operationTimeout,
				application -> {
					undeployApplication(application, operationTimeout);
				},
				appName -> {
					throw new IllegalStateException(new TimeoutException(
							"Application " + appName + " discovery timed-out. Check if it is deployed."));
				}
		);
	}

	public void undeployApplicationIfExists(String name) {
		log.info("Undeploying application {} (if it exists) ...", name);
		doWithApplication(
				name,
				Duration.of(5, ChronoUnit.SECONDS),
				app -> {
					undeployApplication(app, operationTimeout);
				},
				appName -> {
					log.warn("Application {} was not found, could not be undeployed", name);
				});
	}

	public void undeployApplication(@NonNull Application application, Duration timeout) {
		final String applicationName = application.getName();
		log.info("Undeploying application : {}", applicationName);
		application.undeployAndWait(timeout.toMillis(), TimeUnit.MILLISECONDS);
		log.info("{} has been successfully undeployed.", applicationName);
	}

	public void undeployProcessingUnits(@NonNull Predicate<String> processingUnitsNamesPredicate) {
		List<String> allProcessingUnitsNames = this.findAllProcessingUnitsNames();
		allProcessingUnitsNames.stream().filter(processingUnitsNamesPredicate).forEach(puName -> {

			try {
				undeployPu(puName, undeployProcessingUnitTimeout);
			} catch (RuntimeException e) {
				log.error("Failure while undeploying PU {}", puName, e);
			}

		});
	}

	public void doWithApplication(String name, Duration timeout, Consumer<Application> ifFound, Consumer<String> ifNotFound) {
		Application application = admin.getApplications().waitFor(name, timeout.toMillis(), TimeUnit.MILLISECONDS);
		if (application == null) {
			ifNotFound.accept(name);
		} else {
			ifFound.accept(application);
		}
	}

	public void doWithProcessingUnit(String name, Duration timeout, Consumer<ProcessingUnit> ifFound, Consumer<String> ifNotFound) {
		ProcessingUnit processingUnit = admin.getProcessingUnits().waitFor(name, timeout.toMillis(), TimeUnit.MILLISECONDS);
		if (processingUnit == null) {
			ifNotFound.accept(name);
		} else {
			ifFound.accept(processingUnit);
		}
	}

	public void setDefaultTimeout(Duration timeout) {
		log.info("Admin will use a default timeout of {} ms", timeout.toMillis());
		admin.setDefaultTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
	}

}
