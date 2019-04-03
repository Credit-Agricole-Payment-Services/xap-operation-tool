package gca.in.xap.tools.operationtool.predicates.pu;

import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.core.cluster.ClusterInfo;

import java.util.function.Predicate;

@Slf4j
public class IsPrimaryStatefulProcessingUnitPredicate implements Predicate<ProcessingUnitInstance> {

	@Override
	public boolean test(ProcessingUnitInstance pu) {
		final String puName = pu.getName();
		final boolean match = doTest(pu);
		log.debug("pu = {}, match = {}", puName, match);
		return match;
	}

	private boolean doTest(ProcessingUnitInstance pu) {
		ClusterInfo clusterInfo = pu.getClusterInfo();
		boolean hasBackup = clusterInfo.getNumberOfBackups() > 0;
		boolean isBackup = clusterInfo.getBackupId() == null;
		boolean isPrimary = hasBackup && !isBackup;
		return isPrimary;
	}

}
