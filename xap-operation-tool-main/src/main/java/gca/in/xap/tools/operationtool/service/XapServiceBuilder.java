package gca.in.xap.tools.operationtool.service;

import gca.in.xap.tools.operationtool.userinput.UserConfirmationService;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.pu.config.UserDetailsConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Slf4j
@Component
public class XapServiceBuilder {

	private final UserConfirmationService userConfirmationService = new UserConfirmationService();

	private List<String> locators;

	private List<String> groups;

	private UserDetailsConfig userDetails;

	private Duration timeout;

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

	public XapService create() {
		Admin admin = createAdmin();
		GridServiceManagers gridServiceManagers = awaitGSM(admin);

		// the ThreadPool should be large enough
		// in order to execute a task for each machine in the cluster, ideally, at the same time
		ExecutorService executor = Executors.newFixedThreadPool(32);

		XapService result = new XapService();
		result.setAdmin(admin);
		result.setGridServiceManagers(gridServiceManagers);
		result.setOperationTimeout(timeout);
		result.setUserDetails(userDetails);
		result.setExecutorService(executor);
		result.setUserConfirmationService(userConfirmationService);
		return result;
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
