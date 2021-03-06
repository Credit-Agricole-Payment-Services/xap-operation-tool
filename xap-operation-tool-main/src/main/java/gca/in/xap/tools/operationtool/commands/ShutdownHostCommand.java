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

@Component
@CommandLine.Command(name = "shutdown-host")
public class ShutdownHostCommand extends AbstractAppCommand implements Runnable {

	private static final String defaultDemoteMaxSuspendDuration = "PT15S";

	@Autowired
	@Lazy
	private XapService xapService;

	@Autowired
	private IdExtractor idExtractor;

	@Autowired
	private ShutdownHostService shutdownHostService;

	@CommandLine.Option(names = "--skip-relocate-pu", defaultValue = "false", description = "If this is used, Processing Units will not be relocated prior to GSA shutdown. The GSA will be shutdown directly, any GSC that was launched by this GSA will be terminated, so if a PU is running on those GSC, they may be affected. This option is not recommended for production use.")
	private boolean skipRelocateProcessingUnits;

	@CommandLine.Option(names = "--skip-shutdown-agent", defaultValue = "false", description = "If this is used, the GSA will be not be shutdown. If Processing Units needs to be relocated, they are relocated, but in the end, the GSA will not be shutdown.")
	private boolean skipShutdownAgent;

	@CommandLine.Parameters(index = "0", arity = "1", description = "name of the host to shutdown", completionCandidates = HostnamesCandidates.class)
	private InetAddress hostname;

	@CommandLine.Option(
			names = "--demote-max-suspend-time",
			defaultValue = defaultDemoteMaxSuspendDuration,
			description = "The maximum duration the space partition can be suspended while demoting. Default is " + defaultDemoteMaxSuspendDuration + "."
	)
	private Duration demoteMaxSuspendDuration = Duration.parse(defaultDemoteMaxSuspendDuration);

	@PostConstruct
	public void init() {
		HostnamesCandidates.setXapService(this.xapService);
		HostnamesCandidates.setIdExtractor(this.idExtractor);
	}

	@Override
	public void run() {
		xapService.printReportOnContainersAndProcessingUnits();
		xapService.setDefaultTimeout(Duration.ofMinutes(5));
		shutdownHostService.shutdownHost(hostname.getHostName(), skipRelocateProcessingUnits, skipShutdownAgent, demoteMaxSuspendDuration);
	}

}
