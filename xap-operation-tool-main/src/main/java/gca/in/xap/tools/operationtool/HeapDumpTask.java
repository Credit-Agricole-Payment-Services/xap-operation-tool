package gca.in.xap.tools.operationtool;

import gca.in.xap.tools.operationtool.service.UserDetailsConfigFactory;
import gca.in.xap.tools.operationtool.service.XapService;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.pu.config.UserDetailsConfig;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
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

		xapService.printReportOnContainersAndProcessingUnits();

		xapService.setDefaultTimeout(Duration.ofMinutes(5));
		xapService.generateHeapDumpOnEachGsc();
	}

}
