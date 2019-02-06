package gca.in.xap.tools.operationtool;

import gca.in.xap.tools.operationtool.helper.UserDetailsConfigFactory;
import gca.in.xap.tools.operationtool.helper.XapHelper;
import org.openspaces.admin.pu.config.UserDetailsConfig;

import java.util.concurrent.TimeUnit;

public class HeapDumpTask {

	private final UserDetailsConfigFactory userDetailsConfigFactory = new UserDetailsConfigFactory();

	public void doHeapDump(ApplicationArguments applicationArguments) {
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

		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		xapHelper.printReportOnContainersAndProcessingUnits();

		xapHelper.generateHeapDumpOnEachGsc();
	}

}
