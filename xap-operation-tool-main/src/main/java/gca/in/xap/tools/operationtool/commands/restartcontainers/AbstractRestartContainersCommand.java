package gca.in.xap.tools.operationtool.commands.restartcontainers;

import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.service.XapServiceBuilder;
import gca.in.xap.tools.operationtool.util.collectionvisit.CollectionVisitingStrategy;
import gca.in.xap.tools.operationtool.util.collectionvisit.ParallelCollectionVisitingStrategy;
import gca.in.xap.tools.operationtool.util.collectionvisit.SequentialCollectionVisitingStrategy;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import picocli.CommandLine;

import java.time.Duration;
import java.util.function.Predicate;

@Slf4j
public abstract class AbstractRestartContainersCommand extends AbstractAppCommand implements Runnable {

	/**
	 * Default value of 1 minute should be sufficient in most case.
	 * An interval of 2 minutes is too long in some case.
	 * If user wants a longer or shorter duration, user has to use the "--intervalDuration" option
	 */
	private static final String defaultIntervalDuration = "PT1M";

	@Autowired
	@Lazy
	private XapService xapService;

	private final Predicate<GridServiceContainer> predicate;

	@CommandLine.Option(names = "--demoteFirst", defaultValue = "true", negatable = true, description = "In case the Container runs a Primary Stateful Processing Unit, it will ask for a demote of the Space Instance, in order to swap the primary and backup.")
	private boolean demoteFirst;

	@CommandLine.Option(names = "--intervalDuration", defaultValue = defaultIntervalDuration, description = "Interval between each component to restart. Will wait for this interval between each component, to reduce the risk to stress the system when restarting component to quickly. Duration is expressed in ISO_8601 format (example : PT30S for a duration of 30 seconds, PT2M for a duration of 2 minutes). Default value is : " + defaultIntervalDuration)
	private String intervalDuration;

	@CommandLine.Option(names = "--parallel", defaultValue = "false", description = "In this case, the '--intervalDuration' option is ignored. Executes all restarts in parallel (at the same time). This is faster, but this may be dangerous for some usage as it can cause data loss.")
	private boolean parallel;

	public AbstractRestartContainersCommand(Predicate<GridServiceContainer> predicate) {
		this.predicate = predicate;
	}

	@Override
	public void run() {
		final CollectionVisitingStrategy<GridServiceContainer> collectionVisitingStrategy = createRestartStrategy();
		XapServiceBuilder.waitForClusterInfoToUpdate();

		log.info("Report on all GSC :");
		xapService.printReportOnContainersAndProcessingUnits();

		log.info("Report on GSC to restart :");
		xapService.printReportOnContainersAndProcessingUnits(predicate);

		log.info("CollectionVisitingStrategy is : {}", collectionVisitingStrategy);
		xapService.restartContainers(predicate, collectionVisitingStrategy, demoteFirst);
	}

	protected CollectionVisitingStrategy<GridServiceContainer> createRestartStrategy() {
		if (parallel) {
			return new ParallelCollectionVisitingStrategy<>();
		} else {
			return new SequentialCollectionVisitingStrategy<>(Duration.parse(intervalDuration));
		}
	}

}
