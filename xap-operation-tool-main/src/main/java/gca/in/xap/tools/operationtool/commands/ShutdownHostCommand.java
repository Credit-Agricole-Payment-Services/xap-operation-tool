package gca.in.xap.tools.operationtool.commands;

import gca.in.xap.tools.operationtool.service.IdExtractor;
import gca.in.xap.tools.operationtool.service.ShutdownHostService;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Iterator;
import java.util.Set;

@Component
@CommandLine.Command(name = "shutdown-host")
public class ShutdownHostCommand extends AbstractAppCommand implements Runnable {

	private static class HostnamesCandidates implements Iterable<String> {

		private static XapService xapService;

		private static IdExtractor idExtractor;

		private HostnamesCandidates() {
		}

		public Iterator<String> iterator() {
			Set<String> containersHostsNames = idExtractor.extractHostNames(xapService.findAllMachines());
			return containersHostsNames.iterator();
		}
	}


	@Autowired
	@Lazy
	private XapService xapService;

	@Autowired
	private IdExtractor idExtractor;

	@Autowired
	private ShutdownHostService shutdownHostService;

	@CommandLine.Parameters(index = "0", arity = "1", description = "name of the host to shutdown", completionCandidates = HostnamesCandidates.class)
	private InetAddress hostname;

	@PostConstruct
	public void init() {
		HostnamesCandidates.xapService = this.xapService;
		HostnamesCandidates.idExtractor = this.idExtractor;
	}

	@Override
	public void run() {
		xapService.printReportOnContainersAndProcessingUnits();
		xapService.setDefaultTimeout(Duration.ofMinutes(5));
		shutdownHostService.shutdownHost(hostname.getHostName());
	}

}
