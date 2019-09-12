package gca.in.xap.tools.operationtool.commands;

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

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@CommandLine.Command(name = "heapdump")
public class HeapDumpCommand extends AbstractAppCommand implements Runnable {

	/**
	 * Default value of 1 minute should be sufficient in most case.
	 * An interval of 2 minutes is too long in some case.
	 * If user wants a longer or shorter duration, user has to use the "--intervalDuration" option
	 */
	private static final String defaultIntervalDuration = "PT1M";

	@Autowired
	@Lazy
	private XapService xapService;

	@CommandLine.ArgGroup(exclusive = true)
	private ContainersIterationOptions containersIterationOptions;

	@Override
	public void run() {
		final CollectionVisitingStrategy<GridServiceContainer> collectionVisitingStrategy = ContainersIterationOptions.toCollectionVisitingStrategy(containersIterationOptions);

		xapService.printReportOnContainersAndProcessingUnits();
		xapService.setDefaultTimeout(Duration.ofMinutes(5));

		try {
			xapService.generateHeapDumpOnEachContainers(gsc -> true, collectionVisitingStrategy);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
