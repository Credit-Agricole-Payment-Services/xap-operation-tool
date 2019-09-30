package gca.in.xap.tools.operationtool.commands.restartcontainers;

import gca.in.xap.tools.operationtool.commandoptions.CommonContainerFilteringOptions;
import gca.in.xap.tools.operationtool.commandoptions.ContainersIterationOptions;
import gca.in.xap.tools.operationtool.predicates.AndPredicate;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.service.XapServiceBuilder;
import gca.in.xap.tools.operationtool.util.collectionvisit.CollectionVisitingStrategy;
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

	private static final String defaultDemoteMaxSuspendDuration = "PT15S";

	@Autowired
	@Lazy
	private XapService xapService;

	private final Predicate<GridServiceContainer> restartScopePredicate;

	@CommandLine.Option(
			names = "--no-demote-first",
			defaultValue = "true",
			description = "In case the Container runs a Primary Stateful Processing Unit, it will ask for a demote of the Space Instance, in order to swap the primary and backup."
	)
	private Boolean demoteFirst;

	@CommandLine.Option(
			names = "--demote-max-suspend-time",
			defaultValue = defaultDemoteMaxSuspendDuration,
			description = "The maximum duration the space partition can be suspended while demoting. Default is " + defaultDemoteMaxSuspendDuration + "."
	)
	private Duration demoteMaxSuspendDuration = Duration.parse(defaultDemoteMaxSuspendDuration);

	@CommandLine.ArgGroup(exclusive = true)
	private ContainersIterationOptions containersIterationOptions;

	@CommandLine.ArgGroup(exclusive = false)
	private CommonContainerFilteringOptions commonContainerFilteringOptions;

	public AbstractRestartContainersCommand(Predicate<GridServiceContainer> restartScopePredicate) {
		this.restartScopePredicate = restartScopePredicate;
	}

	@Override
	public void run() {
		log.info("demoteFirst = {}", demoteFirst);
		log.info("containersIterationOptions = {}", containersIterationOptions);
		final CollectionVisitingStrategy<GridServiceContainer> collectionVisitingStrategy = ContainersIterationOptions.toCollectionVisitingStrategy(containersIterationOptions);
		final Predicate<GridServiceContainer> predicate = CommonContainerFilteringOptions.toPredicate(commonContainerFilteringOptions);

		XapServiceBuilder.waitForClusterInfoToUpdate();

		log.info("Report on all GSC :");
		xapService.printReportOnContainersAndProcessingUnits();

		log.info("Report on GSC to restart :");
		xapService.printReportOnContainersAndProcessingUnits(this.restartScopePredicate);

		log.info("CollectionVisitingStrategy is : {}", collectionVisitingStrategy);
		xapService.restartContainers(
				new AndPredicate<>(
						predicate,
						this.restartScopePredicate),
				collectionVisitingStrategy,
				demoteFirst,
				demoteMaxSuspendDuration);
	}

}
