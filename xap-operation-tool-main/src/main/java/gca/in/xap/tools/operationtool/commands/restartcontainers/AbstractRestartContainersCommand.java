package gca.in.xap.tools.operationtool.commands.restartcontainers;

import com.kakawait.spring.boot.picocli.autoconfigure.HelpAwarePicocliCommand;
import gca.in.xap.tools.operationtool.XapClientDiscovery;
import gca.in.xap.tools.operationtool.service.RestartStrategy;
import gca.in.xap.tools.operationtool.xapauth.XapClientUserDetailsConfigFactory;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.service.XapServiceBuilder;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.config.UserDetailsConfig;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.function.Predicate;

@Slf4j
public abstract class AbstractRestartContainersCommand extends HelpAwarePicocliCommand implements Runnable {

	@Autowired
	private XapClientUserDetailsConfigFactory xapClientUserDetailsConfigFactory;

	@Autowired
	private XapServiceBuilder xapServiceBuilder;

	@Autowired
	private XapClientDiscovery xapClientDiscovery;

	private final Predicate<GridServiceContainer> predicate;

	private final RestartStrategy restartStrategy;

	public AbstractRestartContainersCommand(Predicate<GridServiceContainer> predicate, RestartStrategy restartStrategy) {
		this.predicate = predicate;
		this.restartStrategy = restartStrategy;
	}

	@Override
	public void run() {
		final UserDetailsConfig userDetails = xapClientUserDetailsConfigFactory.createFromUrlEncodedValue(
				xapClientDiscovery.getUsername(),
				xapClientDiscovery.getPassword()
		);

		final XapService xapService = xapServiceBuilder
				.locators(xapClientDiscovery.getLocators())
				.groups(xapClientDiscovery.getGroups())
				.timeout(xapClientDiscovery.getTimeoutDuration())
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
