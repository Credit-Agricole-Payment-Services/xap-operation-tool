package gca.in.xap.tools.operationtool.predicates.container;

import org.openspaces.admin.gsc.GridServiceContainer;

import java.util.function.Predicate;

public class IsNonEmptyContainerPredicate implements Predicate<GridServiceContainer> {
	@Override
	public boolean test(GridServiceContainer gsc) {
		return gsc.getProcessingUnitInstances().length > 0;
	}
}
