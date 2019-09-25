package gca.in.xap.tools.operationtool.predicates.pu;

import gca.in.xap.tools.operationtool.predicates.space.IsBackupSpaceInstancePredicate;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceInstance;

import java.util.Arrays;
import java.util.function.Predicate;

@Slf4j
public class IsBackupStatefulProcessingUnitPredicate implements Predicate<ProcessingUnitInstance> {

	private final IsBackupSpaceInstancePredicate isBackupSpaceInstancePredicate = new IsBackupSpaceInstancePredicate();

	@Override
	public boolean test(ProcessingUnitInstance pu) {
		final String puName = pu.getName();
		final boolean match = doTest(pu);
		log.debug("pu = {}, match = {}", puName, match);
		return match;
	}

	private boolean doTest(ProcessingUnitInstance pu) {
		SpaceInstance[] spaceInstances = pu.getSpaceInstances();
		if (spaceInstances == null) {
			throw new IllegalStateException("ProcessingUnitInstance should return a non null SpacesInstances array. Is this a unit test that is not properly configured ?");
		}
		return Arrays.stream(spaceInstances).anyMatch(isBackupSpaceInstancePredicate);
	}

}
