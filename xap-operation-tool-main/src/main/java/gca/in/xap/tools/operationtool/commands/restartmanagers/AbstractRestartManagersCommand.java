package gca.in.xap.tools.operationtool.commands.restartmanagers;

import gca.in.xap.tools.operationtool.service.RestartStrategy;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.service.XapServiceBuilder;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsm.GridServiceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.time.Duration;
import java.util.function.Predicate;

@Slf4j
public abstract class AbstractRestartManagersCommand extends AbstractAppCommand implements Runnable {

	static final RestartStrategy noIntervalRestartStrategy = new RestartStrategy(Duration.ZERO);

	static final RestartStrategy defaultIntervalRestartStrategy = new RestartStrategy(Duration.ofMinutes(2));

	@Autowired
	@Lazy
	private XapService xapService;

	private final Predicate<GridServiceManager> predicate;

	private final RestartStrategy restartStrategy;

	public AbstractRestartManagersCommand(Predicate<GridServiceManager> predicate, RestartStrategy restartStrategy) {
		this.predicate = predicate;
		this.restartStrategy = restartStrategy;
	}

	@Override
	public void run() {
		XapServiceBuilder.waitForClusterInfoToUpdate();

		log.info("Report on all GSM :");
		xapService.printReportOnManagers();

		log.info("RestartStrategy is : {}", restartStrategy);

		xapService.restartManagers(predicate, restartStrategy);
	}

}
