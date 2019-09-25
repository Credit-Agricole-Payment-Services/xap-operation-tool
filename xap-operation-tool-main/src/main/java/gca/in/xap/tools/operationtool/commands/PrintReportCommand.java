package gca.in.xap.tools.operationtool.commands;

import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "print-report")
public class PrintReportCommand extends AbstractAppCommand implements Runnable {

	@Autowired
	@Lazy
	private XapService xapService;

	public void run() {
		xapService.printReportOnManagers();
		xapService.printReportOnAgents();
		xapService.printReportOnVirtualMachines();
		xapService.printReportOnContainersAndProcessingUnits();
	}

}
