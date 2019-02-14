package gca.in.xap.tools.operationtool.predicates.pu;

import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.util.function.Predicate;

public class IsStatefulProcessingUnitPredicate implements Predicate<ProcessingUnitInstance> {
	@Override
	public boolean test(ProcessingUnitInstance pu) {
		return pu.isEmbeddedSpaces();
	}
}
