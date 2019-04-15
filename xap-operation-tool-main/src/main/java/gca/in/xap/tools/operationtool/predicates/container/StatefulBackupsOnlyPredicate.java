package gca.in.xap.tools.operationtool.predicates.container;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StatefulBackupsOnlyPredicate extends AbstractStatefullOnlyPredicate {

	@Override
	protected boolean test(String gscId, boolean containsStatefulBackupPU, boolean containsStatefulPrimaryPU) {
		boolean result = containsStatefulBackupPU & !containsStatefulPrimaryPU;
		log.info("GSC {} : containsStatefulBackupPU = {}, containsStatefulPrimaryPU = {}, result = {}", gscId, containsStatefulBackupPU, containsStatefulPrimaryPU, result);
		return result;
	}

}
