package gca.in.xap.tools.operationtool.service.restartstrategy;

import gca.in.xap.tools.operationtool.util.collectionvisit.CollectionVisitingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RestartContainerItemVisitor implements CollectionVisitingStrategy.ItemVisitor<GridServiceContainer> {
	@Override
	public void visit(GridServiceContainer gsc) {
		gsc.restart();
		log.info("GSC {} restarted", gsc.getId());
	}
}
