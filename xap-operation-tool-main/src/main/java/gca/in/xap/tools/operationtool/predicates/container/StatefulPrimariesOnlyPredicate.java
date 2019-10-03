package gca.in.xap.tools.operationtool.predicates.container;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StatefulPrimariesOnlyPredicate extends AbstractStatefullOnlyPredicate {

	@Override
	protected boolean test(String gscId, boolean containsStatefulBackupPU, boolean containsStatefulPrimaryPU) {
		boolean result = containsStatefulPrimaryPU & !containsStatefulBackupPU;
		log.debug("GSC {} : containsStatefulBackupPU = {}, containsStatefulPrimaryPU = {}, result = {}", gscId, containsStatefulBackupPU, containsStatefulPrimaryPU, result);
		return result;
	}

}
