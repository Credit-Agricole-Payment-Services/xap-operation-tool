package gca.in.xap.tools.operationtool.commands.restartmanagers;

import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.service.XapServiceBuilder;
import gca.in.xap.tools.operationtool.service.restartstrategy.ParallelRestartStrategy;
import gca.in.xap.tools.operationtool.service.restartstrategy.RestartStrategy;
import gca.in.xap.tools.operationtool.service.restartstrategy.SequentialRestartStrategy;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import picocli.CommandLine;

import java.time.Duration;
import java.util.function.Predicate;

@Slf4j
public abstract class AbstractRestartManagersCommand extends AbstractAppCommand implements Runnable {

	/**
	 * Default value of 1 minute should be sufficient in most case.
	 * An interval of 2 minutes is too long in some case.
	 * If user wants a longer or shorter duration, user has to use the "--intervalDuration" option
	 */
	private static final String defaultIntervalDuration = "PT1M";

	@Autowired
	@Lazy
	private XapService xapService;

	private final Predicate<GridServiceManager> predicate;

	@CommandLine.Option(names = "--intervalDuration", defaultValue = defaultIntervalDuration, description = "Interval between each component to restart. Will wait for this interval between each component, to reduce the risk to stress the system when restarting component to quickly. Duration is expressed in ISO_8601 format (example : PT30S for a duration of 30 seconds, PT2M for a duration of 2 minutes). Default value is : " + defaultIntervalDuration)
	private String intervalDuration;

	@CommandLine.Option(names = "--parallel", defaultValue = "false", description = "In this case, the '--intervalDuration' option is ignored. Executes all restarts in parallel (at the same time). This is faster, but this may be dangerous for some usage as it can cause data loss.")
	private boolean parallel;

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


	protected RestartStrategy<GridServiceContainer> createRestartStrategy() {
		if (parallel) {
			return new ParallelRestartStrategy<>();
		} else {
			return new SequentialRestartStrategy<>(Duration.parse(intervalDuration));
		}
	}

}
