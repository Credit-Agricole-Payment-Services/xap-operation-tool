package org.github.caps.xap.tools.applicationdeployer;

import org.github.caps.xap.tools.applicationdeployer.helper.UserDetailsConfigFactory;
import org.github.caps.xap.tools.applicationdeployer.helper.XapHelper;
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
