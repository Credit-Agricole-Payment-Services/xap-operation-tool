package gca.in.xap.tools.operationtool.commands;

import com.kakawait.spring.boot.picocli.autoconfigure.HelpAwarePicocliCommand;
import gca.in.xap.tools.operationtool.service.XapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.time.Duration;

@Component
@CommandLine.Command(name = "shutdown-host")
public class ShutdownHostCommand extends HelpAwarePicocliCommand implements Runnable {

	@Autowired
	private XapService xapService;

	@CommandLine.Parameters(index = "0", arity = "1", description = "name of the host to shutdown")
	private String hostname;

	@Override
	public void run() {
		xapService.printReportOnContainersAndProcessingUnits();
		xapService.setDefaultTimeout(Duration.ofMinutes(5));
		xapService.shutdownHost(hostname);
	}

}
