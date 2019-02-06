package gca.in.xap.tools.operationtool;

import gca.in.xap.tools.operationtool.service.UserDetailsConfigFactory;
import gca.in.xap.tools.operationtool.service.XapService;
import org.openspaces.admin.pu.config.UserDetailsConfig;

public class Undeployer {

	private final UserDetailsConfigFactory userDetailsConfigFactory = new UserDetailsConfigFactory();

	public void doUndeploy(ApplicationArguments applicationArguments, String applicationName) {
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
