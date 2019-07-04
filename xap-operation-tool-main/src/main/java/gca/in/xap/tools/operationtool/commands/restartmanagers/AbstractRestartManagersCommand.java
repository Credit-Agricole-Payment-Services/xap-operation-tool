package gca.in.xap.tools.operationtool.commands.restartmanagers;

import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.service.XapServiceBuilder;
import gca.in.xap.tools.operationtool.service.restartstrategy.RestartStrategy;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsm.GridServiceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.function.Predicate;

@Slf4j
public abstract class AbstractRestartManagersCommand extends AbstractAppCommand implements Runnable {

	@Autowired
	@Lazy
	private XapService xapService;

	private final Predicate<GridServiceManager> predicate;

	public AbstractRestartManagersCommand(Predicate<GridServiceManager> predicate) {
		this.predicate = predicate;
	}

	@Override
	public void run() {
		final RestartStrategy restartStrategy = createRestartStrategy();

		XapServiceBuilder.waitForClusterInfoToUpdate();

		log.info("Report on all GSM :");
		xapService.printReportOnManagers();

		log.info("RestartStrategy is : {}", restartStrategy);
		xapService.restartManagers(predicate, restartStrategy);
	}

	protected abstract RestartStrategy<GridServiceManager> createRestartStrategy();

}
