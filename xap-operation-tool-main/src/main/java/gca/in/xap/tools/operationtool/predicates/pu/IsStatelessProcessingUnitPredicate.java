package gca.in.xap.tools.operationtool.predicates.pu;

import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.util.function.Predicate;

@Slf4j
public class IsStatelessProcessingUnitPredicate implements Predicate<ProcessingUnitInstance> {

	private final IsStatefulProcessingUnitPredicate isStatefulProcessingUnitPredicate = new IsStatefulProcessingUnitPredicate();

	@Override
	public boolean test(ProcessingUnitInstance pu) {
		final String puName = pu.getName();
		final boolean match = doTest(pu);
		log.debug("pu = {}, match = {}", puName, match);
		return match;
	}

	private boolean doTest(ProcessingUnitInstance pu) {
		return !isStatefulProcessingUnitPredicate.test(pu);
	}

}
