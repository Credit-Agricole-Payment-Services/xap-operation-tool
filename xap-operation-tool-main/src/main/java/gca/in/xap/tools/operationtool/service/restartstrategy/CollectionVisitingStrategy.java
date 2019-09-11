package gca.in.xap.tools.operationtool.service.restartstrategy;

import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.gsm.GridServiceManager;
import org.openspaces.admin.machine.Machine;


public interface CollectionVisitingStrategy<T> {

	interface ItemVisitor<T> {
		void visit(T item);
	}

	@Slf4j
	class RestartContainerItemVisitor implements ItemVisitor<GridServiceContainer> {
		@Override
		public void visit(GridServiceContainer gsc) {
			gsc.restart();
			log.info("GSC {} restarted", gsc.getId());
		}
	}

	@Slf4j
	class RestartManagerItemVisitor implements ItemVisitor<GridServiceManager> {
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

	@Slf4j
	class RestartAgentItemVisitor implements ItemVisitor<GridServiceAgent> {
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

	void perform(T[] items, ItemVisitor<T> itemVisitor);

}
