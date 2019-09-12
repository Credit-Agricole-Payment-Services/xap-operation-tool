package gca.in.xap.tools.operationtool.commandoptions;

import gca.in.xap.tools.operationtool.util.collectionvisit.CollectionVisitingStrategy;
import gca.in.xap.tools.operationtool.util.collectionvisit.ParallelCollectionVisitingStrategy;
import gca.in.xap.tools.operationtool.util.collectionvisit.SequentialCollectionVisitingStrategy;
import org.openspaces.admin.gsc.GridServiceContainer;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.time.Duration;

public class ContainersIterationOptions {

	public static CollectionVisitingStrategy<GridServiceContainer> toCollectionVisitingStrategy(@Nullable ContainersIterationOptions containersIterationOptions) {
		if (containersIterationOptions == null) {
			containersIterationOptions = new ContainersIterationOptions();
		}
		return containersIterationOptions.toCollectionVisitingStrategy();
	}

	/**
	 * Default value of 1 minute should be sufficient in most case.
	 * An interval of 2 minutes is too long in some case.
	 * If user wants a longer or shorter duration, user has to use the "--intervalDuration" option
	 */
	private final String defaultIntervalDuration = "PT1M";

	@CommandLine.Option(names = "--intervalDuration", defaultValue = defaultIntervalDuration, description = "Interval between each component to process. Will wait for this interval between each component, to reduce the risk to stress the system when operating components to quickly. Duration is expressed in ISO_8601 format (example : PT30S for a duration of 30 seconds, PT2M for a duration of 2 minutes). Default value is : " + defaultIntervalDuration)
	private String intervalDuration = defaultIntervalDuration;

	@CommandLine.Option(names = "--parallel", defaultValue = "false", description = "In this case, the '--intervalDuration' option is ignored. Executes all operations in parallel (at the same time). This is faster, but this may be dangerous for some usage as it can cause data loss.")
	private boolean parallel;

	public CollectionVisitingStrategy<GridServiceContainer> toCollectionVisitingStrategy() {
		if (parallel) {
			return new ParallelCollectionVisitingStrategy<>();
		} else {
			return new SequentialCollectionVisitingStrategy<>(Duration.parse(intervalDuration));
		}
	}

}
