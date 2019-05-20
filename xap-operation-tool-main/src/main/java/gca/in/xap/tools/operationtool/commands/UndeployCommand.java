package gca.in.xap.tools.operationtool.commands;

import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "undeploy")
public class UndeployCommand extends AbstractAppCommand implements Runnable {

	@Autowired
	@Lazy
	private XapService xapService;

	@CommandLine.Parameters(index = "0", arity = "1", description = "Application Name")
	private String applicationName;

	@Override
	public void run() {
		xapService.printReportOnContainersAndProcessingUnits();
		xapService.undeployIfExists(applicationName);
	}

}
