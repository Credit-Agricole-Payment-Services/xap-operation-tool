package gca.in.xap.tools.operationtool.commandoptions;

import gca.in.xap.tools.operationtool.util.collectionvisit.CollectionVisitingStrategy;
import gca.in.xap.tools.operationtool.util.collectionvisit.ParallelCollectionVisitingStrategy;
import gca.in.xap.tools.operationtool.util.collectionvisit.SequentialCollectionVisitingStrategy;
import org.openspaces.admin.gsa.GridServiceAgent;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.time.Duration;

public class AgentsIterationOptions {

	public static CollectionVisitingStrategy<GridServiceAgent> toCollectionVisitingStrategy(@Nullable AgentsIterationOptions containersIterationOptions) {
		if (containersIterationOptions == null) {
			containersIterationOptions = new AgentsIterationOptions();
		}
		return containersIterationOptions.toCollectionVisitingStrategy();
	}

	/**
	 * Default value of 5 seconds should be sufficient in most case.
	 * An interval of 1 minutes is too long in most case.
	 * If user wants a longer or shorter duration, user has to use the "--intervalDuration" option
	 */
	private final String defaultIntervalDuration = "PT5S";

	@CommandLine.Option(names = "--intervalDuration", defaultValue = defaultIntervalDuration, description = "Interval between each component to process. Will wait for this interval between each component, to reduce the risk to stress the system when operating components to quickly. Duration is expressed in ISO_8601 format (example : PT30S for a duration of 30 seconds, PT2M for a duration of 2 minutes). Default value is : " + defaultIntervalDuration)
	private String intervalDuration = defaultIntervalDuration;

	@CommandLine.Option(names = "--parallel", defaultValue = "false", description = "In this case, the '--intervalDuration' option is ignored. Executes all operations in parallel (at the same time). This is faster, but this may be dangerous for some usage as it can cause data loss.")
	private boolean parallel;

	private CollectionVisitingStrategy<GridServiceAgent> toCollectionVisitingStrategy() {
		if (parallel) {
			return new ParallelCollectionVisitingStrategy<>();
		} else {
			return new SequentialCollectionVisitingStrategy<>(Duration.parse(intervalDuration));
		}
	}

}
