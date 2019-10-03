package gca.in.xap.tools.operationtool.commands;

import gca.in.xap.tools.operationtool.model.GlobalAgentId;
import gca.in.xap.tools.operationtool.service.IdExtractor;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.util.List;

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
		xapService.printReportOnAgents();

		agentIds.stream()
				.forEach(agentId ->
				{
					final String hostName = hostNameOrAddress.getHostName();
					xapService.killByGlobalAgentId(new GlobalAgentId(hostName, agentId));
				});
	}

}
