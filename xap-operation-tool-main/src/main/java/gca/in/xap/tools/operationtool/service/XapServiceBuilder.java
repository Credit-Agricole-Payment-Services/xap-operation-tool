package gca.in.xap.tools.operationtool.service;

import gca.in.xap.tools.operationtool.deploymentdescriptors.json.DeploymentDescriptorMarshaller;
import gca.in.xap.tools.operationtool.deploymentdescriptors.puconfig.ProcessingUnitConfigToDeploymentDescriptorMapper;
import gca.in.xap.tools.operationtool.service.deployer.DefaultApplicationDeployer;
import gca.in.xap.tools.operationtool.service.deployer.DefaultProcessingUnitDeployer;
import gca.in.xap.tools.operationtool.service.deployer.HttpProcessingUnitDeployer;
import gca.in.xap.tools.operationtool.service.deployer.ProcessingUnitDeployerType;
import gca.in.xap.tools.operationtool.userinput.UserConfirmationService;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.pu.config.UserDetailsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Slf4j
@Component
public class XapServiceBuilder {

	@Autowired
	private UserConfirmationService userConfirmationService;

	@Autowired
	private IdExtractor idExtractor;

	@Autowired
	private Vertx vertx;

	@Autowired
	private DeploymentDescriptorMarshaller deploymentDescriptorMarshaller;

	@Autowired
	private  ProcessingUnitConfigToDeploymentDescriptorMapper processingUnitConfigToDeploymentDescriptorMapper;

	private List<String> locators;

	private List<String> groups;

	private UserDetailsConfig userDetails;

	private Duration timeout;

	private ProcessingUnitDeployerType processingUnitDeployerType = ProcessingUnitDeployerType.REST_API;

	public XapServiceBuilder locators(List<String> locators) {
		this.locators = locators;
		return this;
	}

	public XapServiceBuilder groups(List<String> groups) {
		this.groups = groups;
		return this;
	}

	public XapServiceBuilder userDetails(UserDetailsConfig userDetails) {
		this.userDetails = userDetails;
		return this;
	}

	public XapServiceBuilder timeout(Duration timeout) {
		this.timeout = timeout;
		return this;
	}

	public XapServiceBuilder processingUnitDeployerType(ProcessingUnitDeployerType processingUnitDeployerType) {
		this.processingUnitDeployerType = processingUnitDeployerType;
		return this;
	}

	void waitToDiscoverXap(long durationInSeconds, int remainingAttemptCount) {
		log.info("Waiting a little bit in order to discover XAP Managers (remainingAttemptCount = {}) ...", remainingAttemptCount);
		try {
			TimeUnit.SECONDS.sleep(durationInSeconds);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean isNullOrEmpty(GridServiceManagers gridServiceManagers) {
		return gridServiceManagers == null || gridServiceManagers.getSize() == 0;
	}

	/**
	 * This methors await for the GSM to be available/connected
	 * It usually return quickly when the GSMs can be contacted quickly, but it may take 2 or 3 seconds in some cases.
	 * <p>
	 * There is a maximum attempt count, after this has been reached, the methods returns null.
	 * So the application will eventually fail later.
	 */
	private GridServiceManagers awaitGSM(Admin admin) {
		GridServiceManagers gridServiceManagers = null;
		final long waitIntervalInSeconds = 1;
		int remainingAttemptCount = 10;
		while (remainingAttemptCount > 0 && isNullOrEmpty(gridServiceManagers)) {
			waitToDiscoverXap(waitIntervalInSeconds, remainingAttemptCount);
			gridServiceManagers = getGridServiceManagersFromAdmin(admin);
			int managersCount = gridServiceManagers.getSize();
			log.info("Found Managers count : {}", managersCount);
			remainingAttemptCount--;
		}
		return gridServiceManagers;
	}

	public static void waitForClusterInfoToUpdate() {
		try {
			log.info("Waiting in order to get a cluster state as accurate as possible ...");
			TimeUnit.MILLISECONDS.sleep(2000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public XapService create() {
		Admin admin = createAdmin();
		awaitGSM(admin);

		waitForClusterInfoToUpdate();

		// the ThreadPool should be large enough
		// in order to execute a task for each machine in the cluster, ideally, at the same time
		ExecutorService executor = newCachedThreadPool(32, new ThreadFactory() {

			private final String threadNamePrefix = XapServiceBuilder.class.getSimpleName();

			private final AtomicInteger counter = new AtomicInteger(0);

			@Override
			public Thread newThread(Runnable runnable) {
				final int threadIndex = counter.incrementAndGet();
				final Thread thread = new Thread(runnable);
				thread.setDaemon(true);
				thread.setName(threadNamePrefix + "-" + String.format("%03d", threadIndex));
				return thread;
			}
		});

		XapService result = new XapService();
		result.setAdmin(admin);
		result.setOperationTimeout(timeout);
		result.setUserDetails(userDetails);
		result.setExecutorService(executor);
		result.setUserConfirmationService(userConfirmationService);
		result.setIdExtractor(idExtractor);
		result.setApplicationDeployer(new DefaultApplicationDeployer(admin));
		switch (processingUnitDeployerType) {
			case JAVA_API:
				result.setProcessingUnitDeployer(new DefaultProcessingUnitDeployer(admin));
				break;
			case REST_API:
				result.setProcessingUnitDeployer(new HttpProcessingUnitDeployer(admin, vertx, deploymentDescriptorMarshaller, processingUnitConfigToDeploymentDescriptorMapper));
				break;
		}
		return result;
	}

	public static ExecutorService newCachedThreadPool(int maxThreadsCount, ThreadFactory threadFactory) {
		return new ThreadPoolExecutor(0, maxThreadsCount,
				60L, TimeUnit.SECONDS,
				new SynchronousQueue<>(),
				threadFactory);
	}

	GridServiceManagers getGridServiceManagersFromAdmin(Admin admin) {
		return admin.getGridServiceManagers();
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
