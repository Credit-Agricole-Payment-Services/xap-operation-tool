package gca.in.xap.tools.operationtool.predicates.container;

public class StatefulPrimariesOnlyPredicate extends AbstractStatefullOnlyPredicate {

	@Override
	protected boolean test(boolean containsStatefulBackupPU, boolean containsStatefulPrimaryPU) {
		return containsStatefulPrimaryPU & !containsStatefulBackupPU;
	}

}
