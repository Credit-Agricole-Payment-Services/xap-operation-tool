package gca.in.xap.tools.operationtool.commands.shutdownagents;

import gca.in.xap.tools.operationtool.commandoptions.AgentsIterationOptions;
import gca.in.xap.tools.operationtool.commandoptions.MachinesFilterOptions;
import gca.in.xap.tools.operationtool.predicates.AndPredicate;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.util.collectionvisit.CollectionVisitingStrategy;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;

@Slf4j
@Component
@CommandLine.Command(name = "shutdown-agents-all")
public class ShutdownAgentsAllCommand extends AbstractAppCommand implements Runnable {

	@Autowired
	@Lazy
	private XapService xapService;

	@CommandLine.ArgGroup(exclusive = false)
	private AgentsIterationOptions agentsIterationOptions;

	@CommandLine.ArgGroup(exclusive = false)
	private MachinesFilterOptions<GridServiceAgent> machinesFilterOptions;

	@Override
	public void run() {
		if (machinesFilterOptions == null) {
			machinesFilterOptions = new MachinesFilterOptions<>();
		}

		final CollectionVisitingStrategy<GridServiceAgent> collectionVisitingStrategy = AgentsIterationOptions.toCollectionVisitingStrategy(agentsIterationOptions);
		log.info("Report on all GSM :");
		xapService.printReportOnManagers();

		xapService.printReportOnContainersAndProcessingUnits();
		xapService.setDefaultTimeout(Duration.ofMinutes(5));

		log.info("CollectionVisitingStrategy is : {}", collectionVisitingStrategy);

		log.info("Shutting down all agents on non-managers hosts ...");
		final List<String> managersHostnames = xapService.findManagersHostnames();

		// do a first iteration on machines that are not running a GSM
		Predicate<GridServiceAgent> firstIterationPredicate = gsa -> !managersHostnames.contains(gsa.getMachine().getHostName());
		xapService.shutdownAgents(
				new AndPredicate<>(
						machinesFilterOptions.toPredicate(),
						firstIterationPredicate),
				collectionVisitingStrategy);

		log.info("Shutting down all agents on managers hosts ...");
		@NonNull Predicate<GridServiceAgent> secondIterationPredicate = gsa -> true;
		xapService.shutdownAgents(
				new AndPredicate<>(
						machinesFilterOptions.toPredicate(),
						secondIterationPredicate),
				collectionVisitingStrategy);
	}

}
