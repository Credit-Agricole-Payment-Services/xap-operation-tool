package gca.in.xap.tools.operationtool.commands;

import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.service.rebalance.RebalanceProcessingUnitService;
import gca.in.xap.tools.operationtool.service.rebalance.ZonesGroup;
import gca.in.xap.tools.operationtool.service.rebalance.ZonesGroups;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.time.Duration;
import java.util.List;

@Component
@CommandLine.Command(name = "rebalance-processing-unit")
@Slf4j
public class RebalanceProcessingUnitCommand extends AbstractAppCommand implements Runnable {

	@Autowired
	@Lazy
	private XapService xapService;

	@Autowired
	private RebalanceProcessingUnitService rebalanceProcessingUnitService;

	@CommandLine.Parameters(index = "0", arity = "1", description = "Name of the ProcessingUnit to re-balance")
	private String processingUnitName;

	@CommandLine.Option(names = {"--onceOnly"}, description = "Only rebalance 1 Processing Unit Instance. If the option is set, then only 1 instance will be relocated. If the option is omited, then it will relocate as many instance os needed until the Processing Unit is balanced.")
	private boolean onceOnly;

	@CommandLine.ArgGroup(exclusive = false, multiplicity = "0..1")
	private List<ZonesGroup> zonesGroupsList;

	@Override
	public void run() {
		final ZonesGroups zonesGroups = new ZonesGroups(zonesGroupsList);
		log.info("zonesGroups = {}", zonesGroups);

		xapService.printReportOnContainersAndProcessingUnits();
		xapService.setDefaultTimeout(Duration.ofMinutes(5));

		rebalanceProcessingUnitService.rebalanceProcessingUnit(processingUnitName, onceOnly, zonesGroups);
	}

}
