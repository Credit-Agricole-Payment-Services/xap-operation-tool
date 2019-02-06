package gca.in.xap.tools.operationtool;

import gca.in.xap.tools.operationtool.service.UserDetailsConfigFactory;
import gca.in.xap.tools.operationtool.service.XapService;
import org.openspaces.admin.pu.config.UserDetailsConfig;

import java.util.concurrent.TimeUnit;

public class HeapDumpTask {

	private final UserDetailsConfigFactory userDetailsConfigFactory = new UserDetailsConfigFactory();

	public void doHeapDump(ApplicationArguments applicationArguments) {
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

		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		xapService.printReportOnContainersAndProcessingUnits();

		xapService.generateHeapDumpOnEachGsc();
	}

}
