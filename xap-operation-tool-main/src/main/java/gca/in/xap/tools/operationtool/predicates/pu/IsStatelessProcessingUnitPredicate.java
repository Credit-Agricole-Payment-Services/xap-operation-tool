package gca.in.xap.tools.operationtool.predicates.pu;

import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.util.function.Predicate;

public class IsStatelessProcessingUnitPredicate implements Predicate<ProcessingUnitInstance> {
	@Override
	public boolean test(ProcessingUnitInstance pu) {
		return !pu.isEmbeddedSpaces();
	}
}
