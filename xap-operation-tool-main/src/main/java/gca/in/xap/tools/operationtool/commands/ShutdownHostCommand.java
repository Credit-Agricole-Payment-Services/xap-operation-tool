package gca.in.xap.tools.operationtool.commands;

import com.kakawait.spring.boot.picocli.autoconfigure.HelpAwarePicocliCommand;
import gca.in.xap.tools.operationtool.service.XapService;
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
public class ShutdownHostCommand extends HelpAwarePicocliCommand implements Runnable {

	private static class HostnamesCandidates implements Iterable<String> {

		private static XapService xapService;

		private HostnamesCandidates() {
		}

		public Iterator<String> iterator() {
			Set<String> containersHostsNames = xapService.findContainersHostsNames();
			return containersHostsNames.iterator();
		}
	}


	@Autowired
	@Lazy
	private XapService xapService;

	@CommandLine.Parameters(index = "0", arity = "1", description = "name of the host to shutdown", completionCandidates = HostnamesCandidates.class)
	private InetAddress hostname;

	@PostConstruct
	public void init() {
		HostnamesCandidates.xapService = this.xapService;
	}

	@Override
	public void run() {
		xapService.printReportOnContainersAndProcessingUnits();
		xapService.setDefaultTimeout(Duration.ofMinutes(5));
		xapService.shutdownHost(hostname.toString());
	}

}