package gca.in.xap.tools.operationtool.commands;

import com.kakawait.spring.boot.picocli.autoconfigure.HelpAwarePicocliCommand;
import gca.in.xap.tools.operationtool.XapClientDiscovery;
import gca.in.xap.tools.operationtool.xapauth.XapClientUserDetailsConfigFactory;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.service.XapServiceBuilder;
import org.openspaces.admin.pu.config.UserDetailsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "print-report")
public class PrintReportCommand extends HelpAwarePicocliCommand implements Runnable {

	@Autowired
	@Lazy
	private XapService xapService;

	public void run() {
		xapService.printReportOnManagers();
		xapService.printReportOnVirtualMachines();
		xapService.printReportOnContainersAndProcessingUnits();
	}

}
