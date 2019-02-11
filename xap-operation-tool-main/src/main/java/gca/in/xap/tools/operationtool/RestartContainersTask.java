package gca.in.xap.tools.operationtool;

import gca.in.xap.tools.operationtool.service.UserDetailsConfigFactory;
import gca.in.xap.tools.operationtool.service.XapService;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.pu.config.UserDetailsConfig;

@Slf4j
public class RestartContainersTask {

	private final UserDetailsConfigFactory userDetailsConfigFactory = new UserDetailsConfigFactory();

	public void executeTask(ApplicationArguments applicationArguments) {
		UserDetailsConfig userDetails = userDetailsConfigFactory.createFromUrlEncodedValue(
				applicationArguments.username,
				applicationArguments.password
		);

		XapService xapService = new XapService.Builder()
				.locators(applicationArguments.locators)
				.groups(applicationArguments.groups)
				.timeout(applicationArguments.timeoutDuration)
				.userDetails(userDetails)
				.create();

		xapService.printReportOnContainersAndProcessingUnits();

		xapService.restartAllContainers();
	}

}
