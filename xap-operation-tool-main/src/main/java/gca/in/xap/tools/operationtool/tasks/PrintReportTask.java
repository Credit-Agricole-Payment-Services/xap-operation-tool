package gca.in.xap.tools.operationtool.tasks;

import gca.in.xap.tools.operationtool.ApplicationArguments;
import gca.in.xap.tools.operationtool.service.UserDetailsConfigFactory;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.service.XapServiceBuilder;
import org.openspaces.admin.pu.config.UserDetailsConfig;

public class PrintReportTask {

	private final UserDetailsConfigFactory userDetailsConfigFactory = new UserDetailsConfigFactory();

	private final XapServiceBuilder xapServiceBuilder = new XapServiceBuilder();

	public void executeTask(ApplicationArguments applicationArguments) {
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

		xapService.printReportOnManagers();
		xapService.printReportOnVirtualMachines();
		xapService.printReportOnContainersAndProcessingUnits();

	}

}
