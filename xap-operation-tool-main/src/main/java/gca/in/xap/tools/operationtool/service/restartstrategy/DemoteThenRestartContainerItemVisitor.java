package gca.in.xap.tools.operationtool.service.restartstrategy;

import gca.in.xap.tools.operationtool.service.DemoteService;
import gca.in.xap.tools.operationtool.util.collectionvisit.CollectionVisitingStrategy;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsc.GridServiceContainer;

import java.time.Duration;

@Slf4j
@AllArgsConstructor
public class DemoteThenRestartContainerItemVisitor implements CollectionVisitingStrategy.ItemVisitor<GridServiceContainer> {

	@NonNull
	private final RestartContainerItemVisitor restartContainerItemVisitor;

	@NonNull
	private final DemoteService demoteService;

	@NonNull
	private final Duration demoteMaxSuspendDuration;

	@Override
	public void visit(GridServiceContainer gsc) {
		demoteService.demotePrimarySpaceInstances(gsc, demoteMaxSuspendDuration);
		restartContainerItemVisitor.visit(gsc);
	}
}
