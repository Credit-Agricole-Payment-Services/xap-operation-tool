package gca.in.xap.tools.operationtool.predicates.pu;

import org.junit.Test;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.core.cluster.ClusterInfo;

import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class IsBackupStatefulProcessingUnitPredicateTest {

	IsBackupStatefulProcessingUnitPredicate predicate = new IsBackupStatefulProcessingUnitPredicate();

	@Test
	public void should_not_match_when_no_backup_on_partition() {
		ClusterInfo clusterInfo = mock(ClusterInfo.class);
		doReturn(0).when(clusterInfo).getNumberOfBackups();
		doReturn(null).when(clusterInfo).getBackupId();

		ProcessingUnitInstance pu = mock(ProcessingUnitInstance.class);
		doReturn(clusterInfo).when(pu).getClusterInfo();

		boolean actualResult = predicate.test(pu);
		assertFalse(actualResult);
	}

	@Test
	public void should_not_match_when_backupid_is_null() {
		ClusterInfo clusterInfo = mock(ClusterInfo.class);
		doReturn(1).when(clusterInfo).getNumberOfBackups();
		doReturn(null).when(clusterInfo).getBackupId();

		ProcessingUnitInstance pu = mock(ProcessingUnitInstance.class);
		doReturn(clusterInfo).when(pu).getClusterInfo();

		boolean actualResult = predicate.test(pu);
		assertTrue(actualResult);
	}

	@Test
	public void should_not_match_when_backupid_is_equal_to_instanceid() {
		ClusterInfo clusterInfo = mock(ClusterInfo.class);
		doReturn(1).when(clusterInfo).getNumberOfBackups();
		doReturn(2).when(clusterInfo).getInstanceId();
		doReturn(2).when(clusterInfo).getBackupId();

		ProcessingUnitInstance pu = mock(ProcessingUnitInstance.class);
		doReturn(clusterInfo).when(pu).getClusterInfo();

		boolean actualResult = predicate.test(pu);
		assertFalse(actualResult);
	}

	@Test
	public void should_not_match_when_backupid_is_notequal_to_instanceid() {
		ClusterInfo clusterInfo = mock(ClusterInfo.class);
		doReturn(1).when(clusterInfo).getNumberOfBackups();
		doReturn(2).when(clusterInfo).getInstanceId();
		doReturn(1).when(clusterInfo).getBackupId();

		ProcessingUnitInstance pu = mock(ProcessingUnitInstance.class);
		doReturn(clusterInfo).when(pu).getClusterInfo();

		boolean actualResult = predicate.test(pu);
		assertFalse(actualResult);
	}

}
