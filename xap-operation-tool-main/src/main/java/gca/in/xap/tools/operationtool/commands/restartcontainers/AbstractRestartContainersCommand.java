package gca.in.xap.tools.operationtool.commands.restartcontainers;

import gca.in.xap.tools.operationtool.commandoptions.ContainersIterationOptions;
import gca.in.xap.tools.operationtool.commandoptions.MachinesFilterOptions;
import gca.in.xap.tools.operationtool.commandoptions.ContainersProcessingUnitFilterOptions;
import gca.in.xap.tools.operationtool.commandoptions.ContainersZonesFilterOptions;
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

import java.util.function.Predicate;

@Slf4j
public abstract class AbstractRestartContainersCommand extends AbstractAppCommand implements Runnable {

	@Autowired
	@Lazy
	private XapService xapService;

	private final Predicate<GridServiceContainer> predicate;

	@CommandLine.Option(
			names = "--no-demote-first",
			defaultValue = "true",
			description = "In case the Container runs a Primary Stateful Processing Unit, it will ask for a demote of the Space Instance, in order to swap the primary and backup."
	)
	private Boolean demoteFirst;

	@CommandLine.ArgGroup(exclusive = true)
	private ContainersIterationOptions containersIterationOptions;

	@CommandLine.ArgGroup(exclusive = false)
	private ContainersZonesFilterOptions containersZonesFilterOptions;

	@CommandLine.ArgGroup(exclusive = false)
	private MachinesFilterOptions<GridServiceContainer> machinesFilterOptions;

	@CommandLine.ArgGroup(exclusive = false)
	private ContainersProcessingUnitFilterOptions containersProcessingUnitFilterOptions;

	public AbstractRestartContainersCommand(Predicate<GridServiceContainer> predicate) {
		this.predicate = predicate;
	}

	@Override
	public void run() {
		log.info("demoteFirst = {}", demoteFirst);
		log.info("containersIterationOptions = {}", containersIterationOptions);

		if (containersZonesFilterOptions == null) {
			containersZonesFilterOptions = new ContainersZonesFilterOptions();
		}
		if (machinesFilterOptions == null) {
			machinesFilterOptions = new MachinesFilterOptions<>();
		}
		if (containersProcessingUnitFilterOptions == null) {
			containersProcessingUnitFilterOptions = new ContainersProcessingUnitFilterOptions();
		}

		CollectionVisitingStrategy<GridServiceContainer> collectionVisitingStrategy = ContainersIterationOptions.toCollectionVisitingStrategy(containersIterationOptions);

		XapServiceBuilder.waitForClusterInfoToUpdate();

		log.info("Report on all GSC :");
		xapService.printReportOnContainersAndProcessingUnits();

		log.info("Report on GSC to restart :");
		xapService.printReportOnContainersAndProcessingUnits(this.predicate);

		log.info("CollectionVisitingStrategy is : {}", collectionVisitingStrategy);
		xapService.restartContainers(
				new AndPredicate<>(
						containersZonesFilterOptions.toPredicate(),
						machinesFilterOptions.toPredicate(),
						containersProcessingUnitFilterOptions.toPredicate(),
						this.predicate),
				collectionVisitingStrategy,
				demoteFirst);
	}

}
