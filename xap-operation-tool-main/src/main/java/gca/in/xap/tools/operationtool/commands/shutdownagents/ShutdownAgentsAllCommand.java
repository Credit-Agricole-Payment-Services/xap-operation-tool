package gca.in.xap.tools.operationtool.commands.shutdownagents;

import gca.in.xap.tools.operationtool.commandoptions.AgentsIterationOptions;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.util.collectionvisit.CollectionVisitingStrategy;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@CommandLine.Command(name = "shutdown-agents-all")
public class ShutdownAgentsAllCommand extends AbstractAppCommand implements Runnable {

	@Autowired
	@Lazy
	private XapService xapService;

	@CommandLine.ArgGroup(exclusive = false)
	private AgentsIterationOptions agentsIterationOptions;

	@Override
	public void run() {
		final CollectionVisitingStrategy<GridServiceAgent> collectionVisitingStrategy = AgentsIterationOptions.toCollectionVisitingStrategy(agentsIterationOptions);
		log.info("Report on all GSM :");
		xapService.printReportOnManagers();

		xapService.printReportOnContainersAndProcessingUnits();
		xapService.setDefaultTimeout(Duration.ofMinutes(5));

		log.info("CollectionVisitingStrategy is : {}", collectionVisitingStrategy);

		log.info("Shutting down all agents on non-managers hosts ...");
		final List<String> managersHostnames = xapService.findManagersHostnames();
		xapService.shutdownAgents(
				gsa -> !managersHostnames.contains(gsa.getMachine().getHostName())
				, collectionVisitingStrategy);
		log.info("Shutting down all agents on managers hosts ...");
		xapService.shutdownAgents(gsa -> true, collectionVisitingStrategy);
	}

}
