package gca.in.xap.tools.operationtool.commands;

import gca.in.xap.tools.operationtool.predicates.machine.MachineWithSameNamePredicate;
import gca.in.xap.tools.operationtool.service.IdExtractor;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.machine.Machine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

@Slf4j
@Component
@CommandLine.Command(name = "kill-by-agent-id")
public class KillByAgentIdCommand extends AbstractAppCommand implements Runnable {

	@Autowired
	@Lazy
	private XapService xapService;

	@Autowired
	private IdExtractor idExtractor;

	@CommandLine.Parameters(index = "0", arity = "1", description = "name of the host of the GSA", completionCandidates = HostnamesCandidates.class)
	private InetAddress hostNameOrAddress;

	@CommandLine.Parameters(index = "1", arity = "1..*", description = "List of AgentIds")
	private List<Integer> agentIds;

	@PostConstruct
	public void init() {
		HostnamesCandidates.setXapService(this.xapService);
		HostnamesCandidates.setIdExtractor(this.idExtractor);
	}

	@Override
	public void run() {
		final Predicate<Machine> machinePredicate = new MachineWithSameNamePredicate(hostNameOrAddress.getHostName());
		GridServiceAgent[] agents = xapService.findAgents();
		Arrays.stream(agents)
				.filter(gsa -> machinePredicate.test(gsa.getMachine()))
				.forEach(gsa -> {
			String gsaHostname = gsa.getMachine().getHostName();
			log.info("Found GSA {}", gsaHostname);
			for (Integer agentId : agentIds) {
				log.info("Killing JVM with agentId {} on {}...", agentId, gsaHostname);
				gsa.killByAgentId(agentId);
			}
		});
	}

}
