package gca.in.xap.tools.operationtool.commands;

import gca.in.xap.tools.operationtool.commandoptions.ContainersIterationOptions;
import gca.in.xap.tools.operationtool.commandoptions.MachinesFilterOptions;
import gca.in.xap.tools.operationtool.commandoptions.ContainersProcessingUnitFilterOptions;
import gca.in.xap.tools.operationtool.commandoptions.ContainersZonesFilterOptions;
import gca.in.xap.tools.operationtool.predicates.AndPredicate;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.util.collectionvisit.CollectionVisitingStrategy;
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
@CommandLine.Command(name = "heapdump")
public class HeapDumpCommand extends AbstractAppCommand implements Runnable {

	@Autowired
	@Lazy
	private XapService xapService;

	@CommandLine.ArgGroup(exclusive = true)
	private ContainersIterationOptions containersIterationOptions;

	@CommandLine.ArgGroup(exclusive = false)
	private ContainersZonesFilterOptions containersZonesFilterOptions;

	@CommandLine.ArgGroup(exclusive = false)
	private MachinesFilterOptions<GridServiceContainer> machinesFilterOptions;

	@CommandLine.ArgGroup(exclusive = false)
	private ContainersProcessingUnitFilterOptions containersProcessingUnitFilterOptions;

	@Override
	public void run() {
		if (containersZonesFilterOptions == null) {
			containersZonesFilterOptions = new ContainersZonesFilterOptions();
		}
		if (machinesFilterOptions == null) {
			machinesFilterOptions = new MachinesFilterOptions<>();
		}
		if (containersProcessingUnitFilterOptions == null) {
			containersProcessingUnitFilterOptions = new ContainersProcessingUnitFilterOptions();
		}

		final CollectionVisitingStrategy<GridServiceContainer> collectionVisitingStrategy = ContainersIterationOptions.toCollectionVisitingStrategy(containersIterationOptions);

		xapService.printReportOnContainersAndProcessingUnits();
		xapService.setDefaultTimeout(Duration.ofMinutes(5));

		try {
			xapService.generateHeapDumpOnEachContainers(
					new AndPredicate<>(
							containersZonesFilterOptions.toPredicate(),
							machinesFilterOptions.toPredicate(),
							containersProcessingUnitFilterOptions.toPredicate()),
					collectionVisitingStrategy);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
