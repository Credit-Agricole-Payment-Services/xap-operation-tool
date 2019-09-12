package gca.in.xap.tools.operationtool.commands;

import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.util.collectionvisit.CollectionVisitingStrategy;
import gca.in.xap.tools.operationtool.util.collectionvisit.ParallelCollectionVisitingStrategy;
import gca.in.xap.tools.operationtool.util.collectionvisit.SequentialCollectionVisitingStrategy;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@CommandLine.Command(name = "threaddump")
public class ThreadDumpCommand extends AbstractAppCommand implements Runnable {

	/**
	 * Default value of 5 seconds should be sufficient in most case.
	 * An interval of 1 minutes is too long in some case.
	 * If user wants a longer or shorter duration, user has to use the "--intervalDuration" option
	 */
	private static final String defaultIntervalDuration = "PT5S";

	@Autowired
	@Lazy
	private XapService xapService;

	@CommandLine.Option(names = "--intervalDuration", defaultValue = defaultIntervalDuration, description = "Interval between each component. Will wait for this interval between each component, to reduce the risk to stress the system. Duration is expressed in ISO_8601 format (example : PT30S for a duration of 30 seconds, PT2M for a duration of 2 minutes). Default value is : " + defaultIntervalDuration)
	private String intervalDuration;

	@CommandLine.Option(names = "--parallel", defaultValue = "false", description = "In this case, the '--intervalDuration' option is ignored. Executes all restarts in parallel (at the same time). This is faster, but this may be dangerous for some usage as it can cause data loss.")
	private boolean parallel;

	@Override
	public void run() {
		final CollectionVisitingStrategy<GridServiceContainer> collectionVisitingStrategy = createRestartStrategy();

		xapService.printReportOnContainersAndProcessingUnits();
		xapService.setDefaultTimeout(Duration.ofMinutes(5));

		try {
			xapService.generateThreadDumpOnContainers(gsc -> true, collectionVisitingStrategy);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private CollectionVisitingStrategy<GridServiceContainer> createRestartStrategy() {
		if (parallel) {
			return new ParallelCollectionVisitingStrategy<>();
		} else {
			return new SequentialCollectionVisitingStrategy<>(Duration.parse(intervalDuration));
		}
	}

}
