package gca.in.xap.tools.operationtool.commands.restartmanagers;

import gca.in.xap.tools.operationtool.commandoptions.ManagersIterationOptions;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.service.XapServiceBuilder;
import gca.in.xap.tools.operationtool.util.collectionvisit.CollectionVisitingStrategy;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsm.GridServiceManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import picocli.CommandLine;

import java.util.function.Predicate;

@Slf4j
public abstract class AbstractRestartManagersCommand extends AbstractAppCommand implements Runnable {

	@Autowired
	@Lazy
	private XapService xapService;

	private final Predicate<GridServiceManager> predicate;

	@CommandLine.ArgGroup(exclusive = false, multiplicity = "1")
	private ManagersIterationOptions managersIterationOptions;

	public AbstractRestartManagersCommand(Predicate<GridServiceManager> predicate) {
		this.predicate = predicate;
	}

	@Override
	public void run() {
		final CollectionVisitingStrategy<GridServiceManager> collectionVisitingStrategy = managersIterationOptions.toCollectionVisitingStrategy();

		XapServiceBuilder.waitForClusterInfoToUpdate();

		log.info("Report on all GSM :");
		xapService.printReportOnManagers();

		log.info("CollectionVisitingStrategy is : {}", collectionVisitingStrategy);
		xapService.restartManagers(predicate, collectionVisitingStrategy);
	}

}
