package gca.in.xap.tools.operationtool.predicates.pu;

import gca.in.xap.tools.operationtool.predicates.space.IsBackupSpaceInstancePredicate;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.util.Arrays;
import java.util.function.Predicate;

@Slf4j
public class HasBackupSpaceInstancePredicate implements Predicate<ProcessingUnitInstance> {

	@Override
	public boolean test(ProcessingUnitInstance pu) {
		final String puName = pu.getName();
		final boolean match = doTest(pu);
		log.debug("pu = {}, match = {}", puName, match);
		return match;
	}

	private boolean doTest(ProcessingUnitInstance pu) {
		return Arrays.stream(pu.getSpaceInstances()).anyMatch(new IsBackupSpaceInstancePredicate());
	}

}
