package gca.in.xap.tools.operationtool.predicates.container;

public class StatefulBackupsOnlyPredicate extends AbstractStatefullOnlyPredicate {

	@Override
	protected boolean test(boolean containsStatefulBackupPU, boolean containsStatefulPrimaryPU) {
		return containsStatefulBackupPU & !containsStatefulPrimaryPU;
	}

}
