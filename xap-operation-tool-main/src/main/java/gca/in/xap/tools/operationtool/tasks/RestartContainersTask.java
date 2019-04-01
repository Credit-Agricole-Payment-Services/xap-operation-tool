package gca.in.xap.tools.operationtool.tasks;

import gca.in.xap.tools.operationtool.ApplicationArguments;
import gca.in.xap.tools.operationtool.service.RestartStrategy;
import gca.in.xap.tools.operationtool.service.UserDetailsConfigFactory;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.service.XapServiceBuilder;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.config.UserDetailsConfig;

import java.util.function.Predicate;

@Slf4j
public class RestartContainersTask {

	private final UserDetailsConfigFactory userDetailsConfigFactory = new UserDetailsConfigFactory();

	private final XapServiceBuilder xapServiceBuilder = new XapServiceBuilder();

	private final Predicate<GridServiceContainer> predicate;

	private final RestartStrategy restartStrategy;

	public RestartContainersTask(Predicate<GridServiceContainer> predicate, RestartStrategy restartStrategy) {
		this.predicate = predicate;
		this.restartStrategy = restartStrategy;
	}

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

		log.info("Report on all GSC :");
		xapService.printReportOnContainersAndProcessingUnits();

		log.info("Report on GSC to restart :");
		xapService.printReportOnContainersAndProcessingUnits(predicate);

		log.info("RestartStrategy is : {}", restartStrategy);

		xapService.restartContainers(predicate, restartStrategy);
	}

}
