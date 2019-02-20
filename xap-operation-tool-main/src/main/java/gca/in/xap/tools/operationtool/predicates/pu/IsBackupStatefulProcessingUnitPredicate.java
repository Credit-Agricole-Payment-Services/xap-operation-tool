package gca.in.xap.tools.operationtool.predicates.pu;

import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.core.cluster.ClusterInfo;

import java.util.function.Predicate;

public class IsBackupStatefulProcessingUnitPredicate implements Predicate<ProcessingUnitInstance> {
	@Override
	public boolean test(ProcessingUnitInstance pu) {
		ClusterInfo clusterInfo = pu.getClusterInfo();
		boolean hasBackup = clusterInfo.getNumberOfBackups() > 0;
		boolean isBackup = clusterInfo.getBackupId() == null;
		return hasBackup && isBackup;
	}
}
