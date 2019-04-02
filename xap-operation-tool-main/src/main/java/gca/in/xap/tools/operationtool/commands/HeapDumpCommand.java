package gca.in.xap.tools.operationtool.commands;

import com.kakawait.spring.boot.picocli.autoconfigure.HelpAwarePicocliCommand;
import gca.in.xap.tools.operationtool.XapClientDiscovery;
import gca.in.xap.tools.operationtool.xapauth.XapClientUserDetailsConfigFactory;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.service.XapServiceBuilder;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.pu.config.UserDetailsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.time.Duration;

@Slf4j
@Component
@CommandLine.Command(name = "heapdump")
public class HeapDumpCommand extends HelpAwarePicocliCommand implements Runnable {

	@Autowired
	private XapService xapService;

	@Override
	public void run() {
		xapService.printReportOnContainersAndProcessingUnits();
		xapService.setDefaultTimeout(Duration.ofMinutes(5));
		xapService.generateHeapDumpOnEachGsc();
	}

}
