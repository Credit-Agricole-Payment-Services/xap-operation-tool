package gca.in.xap.tools.operationtool.service.restartstrategy;

import gca.in.xap.tools.operationtool.util.collectionvisit.CollectionVisitingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RestartManagerItemVisitor implements CollectionVisitingStrategy.ItemVisitor<GridServiceManager> {
	@Override
	public void visit(GridServiceManager gsm) {
		Machine machine = gsm.getMachine();
		String hostname = machine.getHostName();
		String hostAddress = machine.getHostAddress();
		log.info("Asking GSM {} ({}) to restart ...", hostname, hostAddress);
		gsm.restart();
		log.info("GSM {} ({}) restarted", hostname, hostAddress);
	}
}
