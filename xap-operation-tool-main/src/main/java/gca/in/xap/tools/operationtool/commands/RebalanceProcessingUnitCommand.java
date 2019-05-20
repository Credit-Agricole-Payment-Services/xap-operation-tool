package gca.in.xap.tools.operationtool.commands;

import gca.in.xap.tools.operationtool.service.RebalanceProcessingUnitService;
import gca.in.xap.tools.operationtool.service.RestartStrategy;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.time.Duration;

@Component
@CommandLine.Command(name = "rebalance-processing-unit")
public class RebalanceProcessingUnitCommand extends AbstractAppCommand implements Runnable {

	static final RestartStrategy defaultIntervalRestartStrategy = new RestartStrategy(Duration.ofMinutes(2));

	@Autowired
	@Lazy
	private XapService xapService;

	@Autowired
	private RebalanceProcessingUnitService rebalanceProcessingUnitService;

	@CommandLine.Parameters(index = "0", arity = "1", description = "Name of the ProcessingUnit to re-balance")
	private String processingUnitName;

	@Override
	public void run() {
		xapService.printReportOnContainersAndProcessingUnits();
		xapService.setDefaultTimeout(Duration.ofMinutes(5));
		rebalanceProcessingUnitService.rebalanceProcessingUnit(processingUnitName, defaultIntervalRestartStrategy);
	}

}
