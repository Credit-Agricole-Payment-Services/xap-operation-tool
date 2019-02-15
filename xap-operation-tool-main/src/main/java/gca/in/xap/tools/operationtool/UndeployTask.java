package gca.in.xap.tools.operationtool;

import gca.in.xap.tools.operationtool.service.UserDetailsConfigFactory;
import gca.in.xap.tools.operationtool.service.XapService;
import org.openspaces.admin.pu.config.UserDetailsConfig;

public class UndeployTask {

	private final UserDetailsConfigFactory userDetailsConfigFactory = new UserDetailsConfigFactory();

	public void executeTask(ApplicationArguments applicationArguments) {
		String applicationName = applicationArguments.commandLineArgs.get(0);

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

		xapService.undeploy(applicationName);
	}

}
