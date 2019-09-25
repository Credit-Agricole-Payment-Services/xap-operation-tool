package gca.in.xap.tools.operationtool.service.restartstrategy;

import gca.in.xap.tools.operationtool.util.collectionvisit.CollectionVisitingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.machine.Machine;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ShutdownAgentItemVisitor implements CollectionVisitingStrategy.ItemVisitor<GridServiceAgent> {
	@Override
	public void visit(GridServiceAgent gsa) {
		Machine machine = gsa.getMachine();
		String hostname = machine.getHostName();
		String hostAddress = machine.getHostAddress();
		log.info("Asking GSA {} ({}) to shutdown ...", hostname, hostAddress);
		gsa.shutdown();
		log.info("GSA {} ({}) shutdown", hostname, hostAddress);
	}
}
