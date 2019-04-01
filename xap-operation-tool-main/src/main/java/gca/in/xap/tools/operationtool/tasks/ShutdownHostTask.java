package gca.in.xap.tools.operationtool.tasks;

import gca.in.xap.tools.operationtool.ApplicationArguments;
import gca.in.xap.tools.operationtool.service.UserDetailsConfigFactory;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.service.XapServiceBuilder;
import org.openspaces.admin.pu.config.UserDetailsConfig;

import java.time.Duration;

public class ShutdownHostTask {

	private final UserDetailsConfigFactory userDetailsConfigFactory = new UserDetailsConfigFactory();

	private final XapServiceBuilder xapServiceBuilder = new XapServiceBuilder();

	public void executeTask(ApplicationArguments applicationArguments) {
		applicationArguments.checkMinimalNumberOfCommandLineArgs(1);

		final UserDetailsConfig userDetails = userDetailsConfigFactory.createFromUrlEncodedValue(
				applicationArguments.getUsername(),
				applicationArguments.getPassword()
		);

		final XapService xapService = xapServiceBuilder
				.locators(applicationArguments.getLocators())
				.groups(applicationArguments.getGroups())
				.timeout(applicationArguments.getTimeoutDuration())
				.userDetails(userDetails)
				.create();

		xapService.printReportOnContainersAndProcessingUnits();

		xapService.setDefaultTimeout(Duration.ofMinutes(5));
		String hostname = applicationArguments.getCommandLineArgs().get(0);
		xapService.shutdownHost(hostname);
	}

}
