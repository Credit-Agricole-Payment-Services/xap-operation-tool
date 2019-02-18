package gca.in.xap.tools.operationtool;

import gca.in.xap.tools.operationtool.service.UserDetailsConfigFactory;
import gca.in.xap.tools.operationtool.service.XapService;
import org.openspaces.admin.pu.config.UserDetailsConfig;

import java.time.Duration;

public class ShutdownHostTask {

	private final UserDetailsConfigFactory userDetailsConfigFactory = new UserDetailsConfigFactory();

	public void executeTask(ApplicationArguments applicationArguments) {
		applicationArguments.checkMinimalNumberOfCommandLineArgs(1);

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

		xapService.setDefaultTimeout(Duration.ofMinutes(5));
		String hostname = applicationArguments.commandLineArgs.get(0);
		xapService.shutdownHost(hostname);
	}

}
