package gca.in.xap.tools.operationtool.predicates.pu;

import gca.in.xap.tools.operationtool.predicates.space.IsBackupSpaceInstancePredicate;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.util.Arrays;
import java.util.function.Predicate;

public class HasBackupSpaceInstancePredicate implements Predicate<ProcessingUnitInstance> {
	@Override
	public boolean test(ProcessingUnitInstance pu) {
		return Arrays.stream(pu.getSpaceInstances()).anyMatch(new IsBackupSpaceInstancePredicate());
	}
}
