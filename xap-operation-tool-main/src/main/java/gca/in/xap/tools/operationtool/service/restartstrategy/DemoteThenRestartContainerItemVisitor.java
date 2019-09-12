package gca.in.xap.tools.operationtool.service.restartstrategy;

import gca.in.xap.tools.operationtool.service.DefaultDemoteService;
import gca.in.xap.tools.operationtool.util.collectionvisit.CollectionVisitingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DemoteThenRestartContainerItemVisitor implements CollectionVisitingStrategy.ItemVisitor<GridServiceContainer> {

	@Autowired
	private RestartContainerItemVisitor restartContainerItemVisitor;

	@Autowired
	private DefaultDemoteService defaultDemoteService;

	@Override
	public void visit(GridServiceContainer gsc) {
		defaultDemoteService.demotePrimarySpaceInstances(gsc);
		restartContainerItemVisitor.visit(gsc);
	}
}
