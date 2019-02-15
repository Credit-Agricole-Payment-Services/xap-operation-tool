package gca.in.xap.tools.operationtool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gca.in.xap.tools.operationtool.model.HeapDumpReport;
import gca.in.xap.tools.operationtool.predicates.NotPredicate;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.application.config.ApplicationConfig;
import org.openspaces.admin.dump.DumpResult;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.GridServiceContainers;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.machine.Machines;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnits;
import org.openspaces.admin.pu.config.ProcessingUnitConfig;
import org.openspaces.admin.pu.config.UserDetailsConfig;
import org.openspaces.admin.pu.topology.ProcessingUnitConfigHolder;
import org.openspaces.admin.zone.config.ExactZonesConfig;
import org.openspaces.admin.zone.config.RequiredZonesConfig;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

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
			@NonNull final Duration timeout) throws TimeoutException {
		long timeoutTime = deploymentStartTime + timeout.toMillis();

		final String applicationConfigName = applicationConfig.getName();
		log.info("Waiting for application {} to deploy ...", applicationConfigName);

		Set<String> deployedPuNames = new LinkedHashSet<>();

		final ProcessingUnits processingUnits = dataApp.getProcessingUnits();

		// get the pu names in the best order of deployment (regarding dependencies between them)
		final List<String> puNamesInOrderOfDeployment = ApplicationConfigHelper.getPuNamesInOrderOfDeployment(applicationConfig);

		for (String puName : puNamesInOrderOfDeployment) {
			ProcessingUnit pu = processingUnits.getProcessingUnit(puName);
			awaitDeployment(pu, deploymentStartTime, timeout, timeoutTime);
			deployedPuNames.add(puName);
		}

		long appDeploymentEndTime = System.currentTimeMillis();
		long appDeploymentDuration = appDeploymentEndTime - deploymentStartTime;

		log.info("Deployed PUs : {}", deployedPuNames);
		log.info("Application deployed in : {} ms", appDeploymentDuration);
	}

	static void awaitDeployment(ProcessingUnit pu, long deploymentStartTime, @NonNull Duration timeout, long expectedMaximumEndDate) throws TimeoutException {
		String puName = pu.getName();
		final int plannedNumberOfInstances = pu.getPlannedNumberOfInstances();
		log.info("Waiting for PU {} to deploy {} instances ...", puName, plannedNumberOfInstances);

		long remainingDelayUntilTimeout = expectedMaximumEndDate - System.currentTimeMillis();
		if (remainingDelayUntilTimeout < 0L) {
			throw new TimeoutException("Application deployment timed out after " + timeout);
		}
		boolean finished = pu.waitFor(plannedNumberOfInstances, remainingDelayUntilTimeout, TimeUnit.MILLISECONDS);
		if (!finished) {
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

	private final DateTimeFormatter heapDumpsFileNamesDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	@Setter
	private Admin admin;

	@Setter
	private GridServiceManagers gridServiceManagers;

	/**
	 * the timeout of the operation (deployment, undeployment)
	 */
	@Setter
	private Duration operationTimeout = Duration.of(1, ChronoUnit.MINUTES);

	@Setter
	private UserDetailsConfig userDetails;

	@Setter
	private ExecutorService executorService;

	private final ObjectMapper objectMapper = new ObjectMapperFactory().createObjectMapper();

	private final int afterThoughtDurationInSeconds = 5;

	private GridServiceContainer[] findContainers() {
		GridServiceContainers gridServiceContainers = admin.getGridServiceContainers();
		GridServiceContainer[] containers = gridServiceContainers.getContainers();
		// we want the GSCs to be sorted by Id, for readability and reproducibility
		Arrays.sort(containers, Comparator.comparing(GridServiceContainer::getId));
		return containers;
	}

	public void printReportOnContainersAndProcessingUnits() {
		printReportOnContainersAndProcessingUnits(gsc -> true);
	}

	public void printReportOnContainersAndProcessingUnits(Predicate<GridServiceContainer> predicate) {
		GridServiceContainer[] containers = findContainers();
		containers = Arrays.stream(containers).filter(predicate).toArray(GridServiceContainer[]::new);
		final int gscCount = containers.length;
		final Collection<String> containersIds = extractIds(containers);
		log.info("Found {} matching running GSC instances : {}", gscCount, containersIds);
		for (GridServiceContainer gsc : containers) {
			String gscId = gsc.getId();
			ProcessingUnitInstance[] puInstances = gsc.getProcessingUnitInstances();
			final int puCount = puInstances.length;
			final Collection<String> puNames = extractProcessingUnitsNames(puInstances);
			log.info("GSC {} is running {} Processing Units : {}", gscId, puCount, puNames);
		}
	}

	public void printReportOnManagers() {
		final GridServiceManagers managers = admin.getGridServiceManagers();
		final int gsmCount = managers.getSize();
		final Collection<String> managersIds = extractIds(managers);
		log.info("Found {} running GSM instances : {}", gsmCount, managersIds);
	}

	public Collection<String> extractRunningProcessingUnitsNames(GridServiceContainer gsc) {
		ProcessingUnitInstance[] puInstances = gsc.getProcessingUnitInstances();
		return extractProcessingUnitsNames(puInstances);
	}

	private Collection<String> extractProcessingUnitsNames(ProcessingUnitInstance[] puInstances) {
		List<String> names = new ArrayList<>();
		for (ProcessingUnitInstance pu : puInstances) {
			names.add(pu.getName());
		}
		Collections.sort(names);
		return names;
	}

	/**
	 * you may want to restart containers after a PU has been undeployed, in order to make sure no unreleased resources remains.
	 */
	public void restartEmptyContainers() {
		final GridServiceContainer[] containers = findContainers();
		final int gscCount = containers.length;
		final Collection<String> containersIds = extractIds(containers);
		log.info("Found {} running GSC instances : {}", gscCount, containersIds);

		List<GridServiceContainer> containersToRestart = new ArrayList<>();
		for (GridServiceContainer gsc : containers) {
			ProcessingUnitInstance[] puInstances = gsc.getProcessingUnitInstances();
			final int puCount = puInstances.length;
			if (puCount == 0) {
				containersToRestart.add(gsc);
			}
		}
		log.info("Will restart all empty GSC instances : {}", extractIds(containersToRestart));
		for (GridServiceContainer gsc : containersToRestart) {
			gsc.restart();
		}
		log.info("Triggered restart of GSC instances : {}", extractIds(containersToRestart));
	}

	public void restartContainers(@NonNull Predicate<GridServiceContainer> predicate, @NonNull RestartStrategy restartStrategy) {
		GridServiceContainer[] containers = findContainers();
		containers = Arrays.stream(containers).filter(predicate).toArray(GridServiceContainer[]::new);
		final int gscCount = containers.length;
		final Collection<String> containersIds = extractIds(containers);
		log.info("Found {} matching GSC instances : {}", gscCount, containersIds);

		log.info("Will restart {} GSC instances : {}", gscCount, containersIds);
		log.info("Are you sure ? You have {} seconds to use CTRL+C to stop if unsure.", afterThoughtDurationInSeconds);
		try {
			TimeUnit.SECONDS.sleep(afterThoughtDurationInSeconds);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		boolean firstIteration = true;
		for (GridServiceContainer gsc : containers) {
			if (!firstIteration) {
				// we want to wait between each component restart
				// we don't want to wait before first restart, nor after last restart
				restartStrategy.waitBetweenComponent();
			}
			gsc.restart();
			log.info("GSC {} restarted", gsc.getId());
			firstIteration = false;
		}
		log.info("Triggered restart of GSC instances : {}", containersIds);
	}

	public void restartAllManagers() {
		final GridServiceManagers managers = admin.getGridServiceManagers();
		final int gsmCount = managers.getSize();
		final Collection<String> managersIds = extractIds(managers);
		log.info("Found {} running GSM instances : {}", gsmCount, managersIds);

		log.info("Will restart all GSM instances : {}", managersIds);

		final List<Future<?>> taskResults = new ArrayList<>();
		// this can be done in parallel to perform quicker when there are a lot of containers
		Arrays.stream(managers.getManagers()).forEach(gsm -> {
			Future<?> taskResult = executorService.submit(() -> {
				Machine machine = gsm.getMachine();
				String hostname = machine.getHostName();
				String hostAddress = machine.getHostAddress();
				log.info("Asking GSM {} ({}) to restart ...", hostname, hostAddress);
				gsm.restart();
				log.info("Waiting 1 minute for GSM {} ({}) to restart ...", hostname, hostAddress);
				try {
					TimeUnit.MINUTES.sleep(1);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			});
			taskResults.add(taskResult);
		});
		awaitTermination(taskResults);
		log.info("Triggered restart of GSM instances : {}", managersIds);
	}

	public void triggerGarbageCollectorOnEachGsc() {
		final GridServiceContainer[] containers = findContainers();
		final int gscCount = containers.length;
		final Collection<String> containersIds = extractIds(containers);
		log.info("Found {} running GSC instances : {}", gscCount, containersIds);

		final List<Future<?>> taskResults = new ArrayList<>();
		// this can be done in parallel to perform quicker when there are a lot of containers
		Arrays.stream(containers).forEach(gsc -> {
			Future<?> taskResult = executorService.submit(() -> {
				final String gscId = gsc.getId();
				try {
					log.info("Triggering GC on GSC {} ...", gscId);
					gsc.getVirtualMachine().runGc();
				} catch (RuntimeException e) {
					log.error("Failure while triggering Garbage Collector on GSC {}", gscId, e);
				}
			});
			taskResults.add(taskResult);
		});
		awaitTermination(taskResults);
		log.info("Triggered GC on GSC instances : {}", containersIds);
	}

	public void generateHeapDumpOnEachGsc() {
		final GridServiceContainer[] containers = findContainers();
		final int gscCount = containers.length;
		final Collection<String> containersIds = extractIds(containers);
		log.info("Found {} running GSC instances : {}", gscCount, containersIds);


		final File outputDirectory = new File("dumps/heap");
		boolean outputDirectoryCreated = outputDirectory.mkdirs();
		log.debug("outputDirectoryCreated = {]", outputDirectoryCreated);

		final List<Future<?>> taskResults = new ArrayList<>();
		// this can be done in parallel to perform quicker when there are a lot of containers
		Arrays.stream(containers).forEach(gsc -> {
			Future<?> taskResult = executorService.submit(() -> {
				final String gscId = gsc.getId();
				try {
					generateHeapDump(gsc, outputDirectory);
				} catch (RuntimeException | IOException e) {
					log.error("Failure while generating a Heap Dump on GSC {}", gscId, e);
				}
			});
			taskResults.add(taskResult);
		});
		awaitTermination(taskResults);
		log.info("Triggered Heap Dump on GSC instances : {}", containersIds);
	}

	private void generateHeapDump(@NonNull GridServiceContainer gsc, @NonNull final File outputDirectory) throws IOException {
		final String gscId = gsc.getId();

		final Machine machine = gsc.getMachine();

		long pid = gsc.getVirtualMachine().getDetails().getPid();

		ProcessingUnitInstance[] processingUnitInstances = gsc.getProcessingUnitInstances();
		Collection<String> processingUnitsNames = extractProcessingUnitsNames(processingUnitInstances);

		final ZonedDateTime time = ZonedDateTime.now();
		final String dumpFileName = "heapdump-" + gscId + "-" + time.format(heapDumpsFileNamesDateTimeFormatter) + ".zip";
		final String reportFileName = "heapdump-" + gscId + "-" + time.format(heapDumpsFileNamesDateTimeFormatter) + ".json";
		final File dumpFile = new File(outputDirectory, dumpFileName);
		final File reportFile = new File(outputDirectory, reportFileName);
		//

		HeapDumpReport heapDumpReport = new HeapDumpReport();
		heapDumpReport.setGscId(gscId);
		heapDumpReport.setPid(pid);
		heapDumpReport.setStartTime(time);
		heapDumpReport.setHeapDumpFileName(dumpFileName);
		heapDumpReport.setProcessingUnitsNames(new ArrayList<>(processingUnitsNames));
		heapDumpReport.setHostName(machine.getHostName());
		heapDumpReport.setHostAddress(machine.getHostAddress());
		objectMapper.writeValue(reportFile, heapDumpReport);

		//
		log.info("Asking GSC {} for a heap dump ...", gscId);
		final DumpResult dumpResult = gsc.generateDump("Generating a heap dump with XAP operation tool", null, "heap");
		log.info("Downloading heap dump from gsc {} to file {} ...", gscId, dumpFile.getAbsolutePath());
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
		Application dataApp = gridServiceManagers.deploy(applicationConfig, timeout.toMillis(), TimeUnit.MILLISECONDS);
		long deployRequestEndTime = System.currentTimeMillis();
		long deployRequestDuration = deployRequestEndTime - deployRequestStartTime;
		log.info("Requested deployment of application : duration = {} ms", deployRequestDuration);

		if (dataApp == null) {
			throw new DeploymentRequestException("Deployment request failed, GridServiceManagers returned null");
		}

		long deploymentStartTime = deployRequestEndTime;
		awaitDeployment(applicationConfig, dataApp, deploymentStartTime, operationTimeout);
	}

	public void deployProcessingUnits(ApplicationConfig applicationConfig, Duration timeout, boolean restartEmptyContainers) throws TimeoutException {
		log.info("Attempting deployment of application '{}' composed of : {} with a timeout of {}",
				applicationConfig.getName(),
				ApplicationConfigHelper.getPuNamesInOrderOfDeployment(applicationConfig),
				timeout
		);

		final long deploymentStartTime = System.currentTimeMillis();
		final long expectedMaximumEndDate = deploymentStartTime + timeout.toMillis();

		for (ProcessingUnitConfigHolder pu : applicationConfig.getProcessingUnits()) {
			ProcessingUnitConfig processingUnitConfig = pu.toProcessingUnitConfig();
			ProcessingUnitDeployment processingUnitDeployment = new CustomProcessingUnitDeployment(pu.getName(), processingUnitConfig);

			log.debug("processingUnitConfig = {}", processingUnitConfig);
			log.debug("processingUnitDeployment = {}", processingUnitDeployment);

			doWithProcessingUnit(pu.getName(), Duration.of(10, ChronoUnit.SECONDS), existingProcessingUnit -> {
				final int instancesCount = existingProcessingUnit.getInstances().length;
				log.info("Undeploying pu {} ... ({} instances are running on GSCs {})", pu.getName(), instancesCount, extractContainerIds(existingProcessingUnit));
				long startTime = System.currentTimeMillis();
				boolean undeployedSuccessful = existingProcessingUnit.undeployAndWait(1, TimeUnit.MINUTES);
				long endTime = System.currentTimeMillis();
				long duration = endTime - startTime;
				if (undeployedSuccessful) {
					log.info("Undeployed pu {} in {} ms", pu.getName(), duration);
				} else {
					log.warn("Timeout waiting for pu {} to undeploy after {} ms", pu.getName(), duration);
				}
			}, s -> {
				log.info("ProcessingUnit " + pu.getName() + " is not already deployed");
			});

			log.info("Deploying pu {} ...", pu.getName());
			long puDeploymentStartTime = System.currentTimeMillis();
			ProcessingUnit processingUnit = gridServiceManagers.deploy(processingUnitDeployment);
			awaitDeployment(processingUnit, puDeploymentStartTime, timeout, expectedMaximumEndDate);
		}

		long deployRequestEndTime = System.currentTimeMillis();
		long appDeploymentDuration = deployRequestEndTime - deploymentStartTime;

		log.info("Application deployed in: {} ms", appDeploymentDuration);
	}

	public void undeploy(String applicationName) {
		log.info("Launch undeploy of {}, operationTimeout = {}", applicationName, operationTimeout);
		doWithApplication(
				applicationName,
				operationTimeout,
				application -> {
					log.info("Undeploying application : {}", applicationName);
					application.undeployAndWait(operationTimeout.toMillis(), TimeUnit.MILLISECONDS);
					log.info("{} has been successfully undeployed.", applicationName);
				},
				appName -> {
					throw new IllegalStateException(new TimeoutException(
							"Application " + appName + " discovery timed-out. Check if it is deployed."));
				}
		);
	}

	public Collection<String> extractContainerIds(ProcessingUnit existingProcessingUnit) {
		return extractIds(existingProcessingUnit.getGridServiceContainers());
	}

	public Collection<String> extractIds(Collection<GridServiceContainer> containers) {
		Set<String> gscIds = new TreeSet<>();
		for (GridServiceContainer gsc : containers) {
			gscIds.add(gsc.getId());
		}
		return gscIds;
	}

	public Collection<String> extractIds(GridServiceContainer[] containers) {
		Set<String> gscIds = new TreeSet<>();
		for (GridServiceContainer gsc : containers) {
			gscIds.add(gsc.getId());
		}
		return gscIds;
	}

	private Collection<String> extractIds(GridServiceManagers managers) {
		Set<String> gscIds = new TreeSet<>();
		for (GridServiceManager gsm : managers) {
			gscIds.add(gsm.getMachine().getHostName());
		}
		return gscIds;
	}

	public void doWithApplication(String name, Duration timeout, Consumer<Application> ifFound, Consumer<String> ifNotFound) {
		Application application = gridServiceManagers.getAdmin().getApplications().waitFor(name, timeout.toMillis(), TimeUnit.MILLISECONDS);
		if (application == null) {
			ifNotFound.accept(name);
		} else {
			ifFound.accept(application);
		}
	}

	public void doWithProcessingUnit(String name, Duration timeout, Consumer<ProcessingUnit> ifFound, Consumer<String> ifNotFound) {
		ProcessingUnit processingUnit = gridServiceManagers.getAdmin().getProcessingUnits().waitFor(name, timeout.toMillis(), TimeUnit.MILLISECONDS);
		if (processingUnit == null) {
			ifNotFound.accept(name);
		} else {
			ifFound.accept(processingUnit);
		}
	}

	public void undeployIfExists(String name) {
		doWithApplication(
				name,
				Duration.of(5, ChronoUnit.SECONDS),
				app -> {
					final String appName = app.getName();
					undeploy(appName);
				},
				appName -> {
				});
	}

	public void shutdownHost(String hostname) {
		final Predicate<Machine> machinePredicate = machine -> machine.getHostName().equals(hostname) || machine.getHostAddress().equals(hostname);

		final Machines machines = gridServiceManagers.getAdmin().getMachines();
		final Machine[] allMachines = machines.getMachines();
		final Machine[] matchingMachines = Arrays.stream(allMachines).filter(machinePredicate).toArray(Machine[]::new);

		log.info("allMachines.length = {}, matchingMachines.length = {}", allMachines.length, matchingMachines.length);

		boolean forbidWhenOnlyOneHost = false;
		if (forbidWhenOnlyOneHost) {
			if (matchingMachines.length == allMachines.length) {
				String message = "This will effectively shutdown all Machines in the XAP cluster, this is not supported in order to prevent service interruption";
				log.error(message);
				throw new IllegalStateException(message);
			}
		}

		AtomicInteger foundPuInstanceCount = null;

		final int maxRelocateAttemptCount = 5;
		AtomicInteger attemptCount = new AtomicInteger(0);

		while (attemptCount.get() < maxRelocateAttemptCount && (foundPuInstanceCount == null || foundPuInstanceCount.get() > 0)) {
			attemptCount.incrementAndGet();
			//
			final AtomicInteger remainingPuInstanceCount = new AtomicInteger(0);
			Arrays.stream(matchingMachines).forEach(machine -> {

				ProcessingUnitInstance[] processingUnitInstances = machine.getProcessingUnitInstances();
				log.info("Found {} ProcessingUnitInstance¨running on Machine {}", processingUnitInstances.length, machine.getHostName());
				Arrays.stream(processingUnitInstances).forEach(puInstance -> {
					final GridServiceContainer gsc = puInstance.getGridServiceContainer();
					log.info("Processing Unit {} Instance¨{} is running on GSC {}. Relocating to another GSC ...", puInstance.getName(), puInstance.getId(), gsc.getId());
					remainingPuInstanceCount.incrementAndGet();

					relocatePuInstance(puInstance, new NotPredicate<>(machinePredicate));
				});
			});
			//
			foundPuInstanceCount = remainingPuInstanceCount;
		}

		if (foundPuInstanceCount.get() == 0) {
			Arrays.stream(matchingMachines).forEach(machine -> {
				GridServiceAgent gridServiceAgent = machine.getGridServiceAgent();
				log.info("Shutting down GSA {} on Machine {} ...", gridServiceAgent.getUid(), machine.getHostName());
				gridServiceAgent.shutdown();
				log.info("Successfully shut down GSA {} on Machine {}", gridServiceAgent.getUid(), machine.getHostName());
			});
		} else {
			log.info("Found {} ProcessingUnitInstance¨running on Machine {}", foundPuInstanceCount.get(), hostname);
		}

	}

	public void relocatePuInstance(ProcessingUnitInstance puInstance, Predicate<Machine> machinePredicate) {
		final GridServiceContainer gscWherePuIsCurrentlyRunning = puInstance.getGridServiceContainer();
		//
		final ProcessingUnit processingUnit = puInstance.getProcessingUnit();
		final RequiredZonesConfig puRequiredContainerZones = processingUnit.getRequiredContainerZones();
		log.info("Looking for a GSC with Zones configuration that matches : {}", puRequiredContainerZones);

		//
		Predicate<GridServiceContainer> containerPredicate = gsc -> {
			final ExactZonesConfig containerExactZones = gsc.getExactZones();
			return puRequiredContainerZones.isSatisfiedBy(containerExactZones);
		};

		GridServiceContainer[] containers = gridServiceManagers.getAdmin().getGridServiceContainers().getContainers();

		GridServiceContainer container = Arrays.stream(containers)
				.filter(gsc -> machinePredicate.test(gsc.getMachine()))
				.filter(gsc -> !gsc.getId().equals(gscWherePuIsCurrentlyRunning.getId()))
				.filter(containerPredicate)
				.min(Comparator.comparingInt(gsc -> gsc.getProcessingUnitInstances().length))
				.orElseThrow(() -> new UnsupportedOperationException("Did not find any GSC matching requirements, with puRequiredContainerZones = " + puRequiredContainerZones));

		log.info("Identified a matching GSC to relocate the PU instance {} of PU {} : {} (having zone config : {})",
				puInstance.getId(),
				processingUnit.getName(),
				container.getId(),
				container.getExactZones());
		puInstance.relocate(container);
	}


	public static class Builder {

		private List<String> locators;

		private List<String> groups;

		private UserDetailsConfig userDetails;

		private Duration timeout;

		public Builder locators(List<String> locators) {
			this.locators = locators;
			return this;
		}

		public Builder groups(List<String> groups) {
			this.groups = groups;
			return this;
		}

		public Builder userDetails(UserDetailsConfig userDetails) {
			this.userDetails = userDetails;
			return this;
		}

		public Builder timeout(Duration timeout) {
			this.timeout = timeout;
			return this;
		}

		void waitToDiscoverXap(int durationInSeconds) {
			log.info("Waiting a little bit in order to discover XAP Managers ...");
			try {
				TimeUnit.SECONDS.sleep(5);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		public XapService create() {
			Admin admin = createAdmin();
			GridServiceManagers gridServiceManagers = null;
			int attemptCount = 10;
			while (attemptCount > 0 && gridServiceManagers == null || gridServiceManagers.getSize() == 0) {
				waitToDiscoverXap(1);
				gridServiceManagers = getGridServiceManagersFromAdmin(admin);
				log.info("GridServiceManagers : {}", Arrays.toString(gridServiceManagers.getManagers()));
				attemptCount--;
			}

			ExecutorService executor = Executors.newFixedThreadPool(4);

			XapService result = new XapService();
			result.setAdmin(admin);
			result.setGridServiceManagers(gridServiceManagers);
			result.setOperationTimeout(timeout);
			result.setUserDetails(userDetails);
			result.setExecutorService(executor);
			return result;
		}

		GridServiceManagers getGridServiceManagersFromAdmin(Admin admin) {
			GridServiceManagers result = admin.getGridServiceManagers();
			final int gsmCount = result.getSize();
			log.info("gsmCount = {}", gsmCount);
			return result;
			//GridServiceManager gridServiceManagers = admin.getGridServiceManagers().waitForAtLeastOne(5, TimeUnit.MINUTES);
			//log.info("Retrieved GridServiceManager> locators: {} ; groups: {}");
			//return gridServiceManagers;
		}

		private Admin createAdmin() {
			AdminFactory factory = new AdminFactory().useDaemonThreads(true).useGsLogging(true);

			if (locators != null) {
				for (String locator : locators) {
					if (!locator.isEmpty()) {
						factory.addLocator(locator);
					}
				}
			}
			if (groups != null) {
				for (String group : groups) {
					if (!group.isEmpty()) {
						factory.addGroup(group);
					}
				}
			}
			if (userDetails != null) {
				factory = factory.credentials(userDetails.getUsername(), userDetails.getPassword());
			}

			Admin admin = factory.createAdmin();
			log.info("Admin will use a default timeout of {} ms", timeout.toMillis());
			admin.setDefaultTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);

			List<String> locators = stream(admin.getLocators()).map(l -> l.getHost() + ":" + l.getPort()).collect(Collectors.toList());
			List<String> groups = stream(admin.getGroups()).collect(Collectors.toList());
			log.info("Using Admin : locators = {} ; groups = {}", locators, groups);
			return admin;
		}

	}

	public void setDefaultTimeout(Duration timeout) {
		log.info("Admin will use a default timeout of {} ms", timeout.toMillis());
		admin.setDefaultTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
	}

}
