package gca.in.xap.tools.operationtool.service;

import gca.in.xap.tools.operationtool.userinput.UserConfirmationService;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.Admin;
import org.openspaces.admin.AdminFactory;
import org.openspaces.admin.gsm.GridServiceManagers;
import org.openspaces.admin.pu.config.UserDetailsConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
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

	void waitToDiscoverXap(int durationInSeconds) {
		log.info("Waiting a little bit in order to discover XAP Managers ...");
		try {
			TimeUnit.SECONDS.sleep(durationInSeconds);
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
		result.setUserConfirmationService(userConfirmationService);
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
