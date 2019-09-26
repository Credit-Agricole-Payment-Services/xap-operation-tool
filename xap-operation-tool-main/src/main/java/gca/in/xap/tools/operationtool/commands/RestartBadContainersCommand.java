package gca.in.xap.tools.operationtool.commands;

import com.gigaspaces.grid.gsa.AgentProcessDetails;
import gca.in.xap.tools.operationtool.model.ComponentType;
import gca.in.xap.tools.operationtool.model.GlobalAgentId;
import gca.in.xap.tools.operationtool.model.GlobalProcessId;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.userinput.UserConfirmationService;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.vm.VirtualMachine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@CommandLine.Command(name = "restart-bad-containers")
public class RestartBadContainersCommand extends AbstractAppCommand implements Runnable {

	@Autowired
	@Lazy
	private XapService xapService;

	@Autowired
	@Setter
	private UserConfirmationService userConfirmationService;

	@Override
	public void run() {
		xapService.printReportOnManagers();
		xapService.printReportOnAgents();
		xapService.printReportOnVirtualMachines();
		xapService.printReportOnContainersAndProcessingUnits();


		// bad containers are defined as follow :

		List<GlobalProcessId> allGridServicesIds = new ArrayList<>();
		final GridServiceAgent[] agents = xapService.findAgents();
		for (GridServiceAgent gsa : agents) {
			String hostName = gsa.getMachine().getHostName();
			AgentProcessDetails[] processDetailsList = gsa.getProcessesDetails().getProcessDetails();
			for (AgentProcessDetails agentProcessDetails : processDetailsList) {
				allGridServicesIds.add(new GlobalProcessId(hostName, agentProcessDetails.getProcessId()));
			}
		}
		Collections.sort(allGridServicesIds);
		log.info("Found {} JVMs managed by the Grid Service Agents : {}", allGridServicesIds.size(), allGridServicesIds);

		// we list all JVMs that are either GSC or UNKNOWN
		List<GlobalProcessId> allJvmIds = Arrays.stream(xapService.findAllVirtualMachines())
				.map(this::toProcessIdentifier)
				.collect(Collectors.toList());
		Collections.sort(allJvmIds);

		log.info("Found {} JVMs in the Grid : {}", allJvmIds.size(), allJvmIds);

		// we list all JVMs that are either a GSA or a GSM
		List<GlobalProcessId> allAgentsAndManagersJvmIds = Arrays.stream(xapService.findAllVirtualMachines())
				.filter(jvm -> {
					@NonNull ComponentType componentType = xapService.guessComponentType(jvm);
					return componentType.equals(ComponentType.GSA) || componentType.equals(ComponentType.GSM);
				})
				.map(this::toProcessIdentifier)
				.collect(Collectors.toList());
		Collections.sort(allAgentsAndManagersJvmIds);

		log.info("Found {} JVMs that are either GSA or GSM : {}", allAgentsAndManagersJvmIds.size(), allAgentsAndManagersJvmIds);

		// we list all GSCs and get the Uid of their JVM
		List<GlobalProcessId> allValidGscIds = Arrays.stream(xapService.findContainers())
				.map(gsc -> toProcessIdentifier(gsc.getVirtualMachine()))
				.collect(Collectors.toList());

		Collections.sort(allValidGscIds);
		log.info("Found {} GSCs : {}", allValidGscIds.size(), allValidGscIds);

		// when a JVM is visible in one list but not in the other list
		// we search for the difference between the 2 lists
		Set<GlobalProcessId> badGridServices = new LinkedHashSet<>(allGridServicesIds);
		badGridServices.addAll(allJvmIds);
		badGridServices.removeAll(allAgentsAndManagersJvmIds);
		badGridServices.removeAll(allValidGscIds);

		log.info("Found {} JVM that seem in a bad condition : {}", badGridServices.size(), badGridServices);

		userConfirmationService.askConfirmationAndWait();

		// we try to restart the GSC of every JVM that seem bad
		badGridServices.stream()
				.forEach(globalProcessId -> {
					log.info("Searching for GSC matching {} ...", globalProcessId);
					GridServiceContainer gsc = xapService.findContainerByGlobalProcessIdentifier(globalProcessId);
					if (gsc != null) {
						log.info("Restarting GSC {} ...", gsc.getId());
						gsc.restart();
					} else {
						GlobalAgentId globalAgentId = xapService.findGlobalAgentIdByGlobalProcessIdentifier(globalProcessId);
						try {
							xapService.killByGlobalAgentId(globalAgentId);
						} catch (RuntimeException e) {
							log.warn("Failed to restart the GSC from JVM {}, and failed to kill it by AgentId restart it. You should log into the host {} and kill process {} manually", globalProcessId.getHostName(), globalProcessId.getPid());
						}
					}
				});

	}

	private GlobalProcessId toProcessIdentifier(VirtualMachine jvm) {
		return new GlobalProcessId(jvm.getMachine().getHostName(), jvm.getDetails().getPid());
	}

}
