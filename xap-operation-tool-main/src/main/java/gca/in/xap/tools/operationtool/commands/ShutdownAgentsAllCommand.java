package gca.in.xap.tools.operationtool.commands;

import com.kakawait.spring.boot.picocli.autoconfigure.HelpAwarePicocliCommand;
import gca.in.xap.tools.operationtool.service.RestartStrategy;
import gca.in.xap.tools.operationtool.service.XapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@CommandLine.Command(name = "shutdown-agents-all")
public class ShutdownAgentsAllCommand extends HelpAwarePicocliCommand implements Runnable {

	static final RestartStrategy noIntervalRestartStrategy = new RestartStrategy(Duration.ZERO);

	@Autowired
	@Lazy
	private XapService xapService;

	@Override
	public void run() {
		log.info("Report on all GSM :");
		xapService.printReportOnManagers();

		xapService.printReportOnContainersAndProcessingUnits();
		xapService.setDefaultTimeout(Duration.ofMinutes(5));

		log.info("RestartStrategy is : {}", noIntervalRestartStrategy);

		log.info("Shutting down all agents on non-managers hosts ...");
		final List<String> managersHostnames = xapService.findManagersHostnames();
		xapService.shutdownAgents(
				gsa -> !managersHostnames.contains(gsa.getMachine().getHostName())
				, noIntervalRestartStrategy);
		log.info("Shutting down all agents on managers hosts ...");
		xapService.shutdownAgents(gsa -> true, noIntervalRestartStrategy);
	}

}
