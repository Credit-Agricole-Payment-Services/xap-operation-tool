package gca.in.xap.tools.operationtool.predicates.pu;

import gca.in.xap.tools.operationtool.predicates.AndPredicate;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.util.function.Predicate;

public class PredicatesFactory {

	public static Predicate<ProcessingUnitInstance> containsStatefulBackupPUPredicate() {
		return new AndPredicate<>(new IsStatefulProcessingUnitPredicate(), new IsBackupStatefulProcessingUnitPredicate(), new HasBackupSpaceInstancePredicate());
	}

	public static Predicate<ProcessingUnitInstance> containsStatefulPrimaryPUPredicate() {
		return new AndPredicate<>(new IsStatefulProcessingUnitPredicate(), new IsPrimaryStatefulProcessingUnitPredicate(), new HasPrimarySpaceInstancePredicate());
	}

}
