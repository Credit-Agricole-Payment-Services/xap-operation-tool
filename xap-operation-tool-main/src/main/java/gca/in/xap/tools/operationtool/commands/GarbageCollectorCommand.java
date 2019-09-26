package gca.in.xap.tools.operationtool.commands;

import gca.in.xap.tools.operationtool.commandoptions.CommonContainerFilteringOptions;
import gca.in.xap.tools.operationtool.commandoptions.ContainersIterationOptions;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.util.collectionvisit.CollectionVisitingStrategy;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.time.Duration;
import java.util.function.Predicate;

@Slf4j
@Component
@CommandLine.Command(name = "trigger-gc")
public class GarbageCollectorCommand extends AbstractAppCommand implements Runnable {

	@Autowired
	@Lazy
	private XapService xapService;

	@CommandLine.ArgGroup(exclusive = true)
	private ContainersIterationOptions containersIterationOptions;

	@CommandLine.ArgGroup(exclusive = false)
	private CommonContainerFilteringOptions commonContainerFilteringOptions;

	@Override
	public void run() {
		final CollectionVisitingStrategy<GridServiceContainer> collectionVisitingStrategy = ContainersIterationOptions.toCollectionVisitingStrategy(containersIterationOptions);
		final Predicate<GridServiceContainer> predicate = CommonContainerFilteringOptions.toPredicate(commonContainerFilteringOptions);

		xapService.printReportOnContainersAndProcessingUnits();
		xapService.setDefaultTimeout(Duration.ofMinutes(2));

		xapService.triggerGarbageCollectorOnContainers(
				predicate,
				collectionVisitingStrategy);
	}

}
