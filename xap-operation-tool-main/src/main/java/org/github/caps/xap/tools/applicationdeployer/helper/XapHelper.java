package org.github.caps.xap.tools.applicationdeployer.helper;

import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.application.Application;
import org.openspaces.admin.application.config.ApplicationConfig;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsc.GridServiceContainers;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitDeployment;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnits;
import org.openspaces.admin.pu.config.ProcessingUnitConfig;
import org.openspaces.admin.pu.config.UserDetailsConfig;
import org.openspaces.admin.pu.topology.ProcessingUnitConfigHolder;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Slf4j
public class XapHelper {

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
			log.info("PU {} deployed successfully after {} ms", puName, durationSince(deploymentStartTime));
		}

		long appDeploymentEndTime = System.currentTimeMillis();
		long appDeploymentDuration = appDeploymentEndTime - deploymentStartTime;

		log.info("Deployed PUs: {}", deployedPuNames);
		log.info("Application deployed in: {} ms", appDeploymentDuration);
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
		log.info("PU {} deployed in {} ms, now has {} running instances", puName, deploymentDuration, currentInstancesCount);

	}

	private static long durationSince(long time) {
		return System.currentTimeMillis() - time;
	}

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

	public void printReportOnContainersAndProcessingUnits() {
		GridServiceContainers gridServiceContainers = admin.getGridServiceContainers();
		GridServiceContainer[] containers = gridServiceContainers.getContainers();
		final int gscCount = containers.length;
		final Collection<String> containersIds = extractIds(containers);
		log.info("Found {} running GSC instances : {}", gscCount, containersIds);
		for (GridServiceContainer gsc : containers) {
			String gscId = gsc.getId();
			ProcessingUnitInstance[] puInstances = gsc.getProcessingUnitInstances();
			final int puCount = puInstances.length;
			final Collection<String> puNames = extractProcessingUnitsNames(puInstances);
			log.info("GSC {} is running {} Processing Units : {}", gscId, puCount, puNames);
		}
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
		GridServiceContainers gridServiceContainers = admin.getGridServiceContainers();
		GridServiceContainer[] containers = gridServiceContainers.getContainers();
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

			log.info("processingUnitConfig = {}", processingUnitConfig);
			log.info("processingUnitDeployment = {}", processingUnitDeployment);

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
		log.info("Launch undeploy of: {} (timeout: {})", applicationName, operationTimeout);
		doWithApplication(
				applicationName,
				operationTimeout,
				application -> {
					log.info("Undeploying application: {}", applicationName);
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

		public XapHelper create() {
			Admin admin = createAdmin();
			GridServiceManagers gridServiceManagers = getGridServiceManagersFromAdmin(admin);
			log.info("GridServiceManagers: {}", Arrays.toString(gridServiceManagers.getManagers()));
			XapHelper result = new XapHelper();
			result.setAdmin(admin);
			result.setGridServiceManagers(gridServiceManagers);
			result.setOperationTimeout(timeout);
			result.setUserDetails(userDetails);
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
}
