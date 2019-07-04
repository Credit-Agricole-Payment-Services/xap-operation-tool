package gca.in.xap.tools.operationtool.commands;

import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.service.restartstrategy.ParallelRestartStrategy;
import gca.in.xap.tools.operationtool.service.restartstrategy.RestartStrategy;
import gca.in.xap.tools.operationtool.service.restartstrategy.SequentialRestartStrategy;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@CommandLine.Command(name = "shutdown-agents-all")
public class ShutdownAgentsAllCommand extends AbstractAppCommand implements Runnable {

	/**
	 * Default value of 1 second should be sufficient in most case.
	 * An interval of 1 minutes is too long in most case.
	 * If user wants a longer or shorter duration, user has to use the "--intervalDuration" option
	 */
	private static final String defaultIntervalDuration = "PT1S";

	@Autowired
	@Lazy
	private XapService xapService;

	@CommandLine.Option(names = "--intervalDuration", defaultValue = defaultIntervalDuration, description = "Interval between each component to restart. Will wait for this interval between each component, to reduce the risk to stress the system when restarting component to quickly. Duration is expressed in ISO_8601 format (example : PT30S for a duration of 30 seconds, PT2M for a duration of 2 minutes). Default value is : " + defaultIntervalDuration)
	private String intervalDuration;

	@CommandLine.Option(names = "--parallel", defaultValue = "false", description = "In this case, the '--intervalDuration' option is ignored. Executes all restarts in parallel (at the same time). This is faster, but this may be dangerous for some usage as it can cause data loss.")
	private boolean parallel;

	@Override
	public void run() {
		final RestartStrategy<GridServiceAgent> restartStrategy = createRestartStrategy();
		log.info("Report on all GSM :");
		xapService.printReportOnManagers();

		xapService.printReportOnContainersAndProcessingUnits();
		xapService.setDefaultTimeout(Duration.ofMinutes(5));

		log.info("RestartStrategy is : {}", restartStrategy);

		log.info("Shutting down all agents on non-managers hosts ...");
		final List<String> managersHostnames = xapService.findManagersHostnames();
		xapService.shutdownAgents(
				gsa -> !managersHostnames.contains(gsa.getMachine().getHostName())
				, restartStrategy);
		log.info("Shutting down all agents on managers hosts ...");
		xapService.shutdownAgents(gsa -> true, restartStrategy);
	}

	protected RestartStrategy<GridServiceAgent> createRestartStrategy() {
		if (parallel) {
			return new ParallelRestartStrategy<>();
		} else {
			return new SequentialRestartStrategy<>(Duration.parse(intervalDuration));
		}
	}

}
