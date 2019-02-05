package gca.in.xap.tools.operationtool;

import gca.in.xap.tools.operationtool.helper.UserDetailsConfigFactory;
import gca.in.xap.tools.operationtool.helper.XapHelper;
import org.openspaces.admin.pu.config.UserDetailsConfig;

public class Undeployer {

	private final UserDetailsConfigFactory userDetailsConfigFactory = new UserDetailsConfigFactory();

	public void doUndeploy(ApplicationArguments applicationArguments, String applicationName) {
		UserDetailsConfig userDetails = userDetailsConfigFactory.createFromUrlEncodedValue(
				applicationArguments.username,
				applicationArguments.password
		);

		XapHelper xapHelper = new XapHelper.Builder()
				.locators(applicationArguments.locators)
				.groups(applicationArguments.groups)
				.timeout(applicationArguments.timeoutDuration)
				.userDetails(userDetails)
				.create();

		xapHelper.undeploy(applicationName);
	}

}
