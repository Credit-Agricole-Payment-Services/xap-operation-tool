package gca.in.xap.tools.operationtool.comparators.processingunitinstance;

import gca.in.xap.tools.operationtool.predicates.pu.IsBackupStatefulProcessingUnitPredicate;
import org.junit.Test;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertSame;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class BackupFirstProcessingUnitInstanceComparatorTest {

	@Test
	public void should_sort_backups_first() {
		ProcessingUnitInstance instance1 = mock(ProcessingUnitInstance.class);
		ProcessingUnitInstance instance2 = mock(ProcessingUnitInstance.class);

		IsBackupStatefulProcessingUnitPredicate predicate = mock(IsBackupStatefulProcessingUnitPredicate.class);
		doReturn(false).when(predicate).test(instance1);
		doReturn(true).when(predicate).test(instance2);

		BackupFirstProcessingUnitInstanceComparator comparator = new BackupFirstProcessingUnitInstanceComparator();
		comparator.setPredicate(predicate);

		final List<ProcessingUnitInstance> list = new ArrayList<>();
		list.add(instance1);
		list.add(instance2);

		list.sort(comparator);

		assertSame(instance2, list.get(0));
		assertSame(instance1, list.get(1));
	}

	@Test
	public void should_keep_backups_first() {
		ProcessingUnitInstance instance1 = mock(ProcessingUnitInstance.class);
		ProcessingUnitInstance instance2 = mock(ProcessingUnitInstance.class);

		IsBackupStatefulProcessingUnitPredicate predicate = mock(IsBackupStatefulProcessingUnitPredicate.class);
		doReturn(true).when(predicate).test(instance1);
		doReturn(false).when(predicate).test(instance2);

		BackupFirstProcessingUnitInstanceComparator comparator = new BackupFirstProcessingUnitInstanceComparator();
		comparator.setPredicate(predicate);

		final List<ProcessingUnitInstance> list = new ArrayList<>();
		list.add(instance1);
		list.add(instance2);

		list.sort(comparator);

		assertSame(instance1, list.get(0));
		assertSame(instance2, list.get(1));
	}

	@Test
	public void should_keep_when_only_backup_found() {
		ProcessingUnitInstance instance1 = mock(ProcessingUnitInstance.class);
		ProcessingUnitInstance instance2 = mock(ProcessingUnitInstance.class);

		IsBackupStatefulProcessingUnitPredicate predicate = mock(IsBackupStatefulProcessingUnitPredicate.class);
		doReturn(true).when(predicate).test(instance1);
		doReturn(true).when(predicate).test(instance2);

		BackupFirstProcessingUnitInstanceComparator comparator = new BackupFirstProcessingUnitInstanceComparator();
		comparator.setPredicate(predicate);

		final List<ProcessingUnitInstance> list = new ArrayList<>();
		list.add(instance1);
		list.add(instance2);

		list.sort(comparator);

		assertSame(instance1, list.get(0));
		assertSame(instance2, list.get(1));
	}

	@Test
	public void should_keep_when_only_primary_found() {
		ProcessingUnitInstance instance1 = mock(ProcessingUnitInstance.class);
		ProcessingUnitInstance instance2 = mock(ProcessingUnitInstance.class);

		IsBackupStatefulProcessingUnitPredicate predicate = mock(IsBackupStatefulProcessingUnitPredicate.class);
		doReturn(false).when(predicate).test(instance1);
		doReturn(false).when(predicate).test(instance2);

		BackupFirstProcessingUnitInstanceComparator comparator = new BackupFirstProcessingUnitInstanceComparator();
		comparator.setPredicate(predicate);

		final List<ProcessingUnitInstance> list = new ArrayList<>();
		list.add(instance1);
		list.add(instance2);

		list.sort(comparator);

		assertSame(instance1, list.get(0));
		assertSame(instance2, list.get(1));
	}

	@Test
	public void should_sort_multiple_instances_of_primaries_and_backups() {
		ProcessingUnitInstance instance1 = mock(ProcessingUnitInstance.class);
		ProcessingUnitInstance instance2 = mock(ProcessingUnitInstance.class);
		ProcessingUnitInstance instance3 = mock(ProcessingUnitInstance.class);
		ProcessingUnitInstance instance4 = mock(ProcessingUnitInstance.class);

		IsBackupStatefulProcessingUnitPredicate predicate = mock(IsBackupStatefulProcessingUnitPredicate.class);
		doReturn(false).when(predicate).test(instance1);
		doReturn(false).when(predicate).test(instance2);
		doReturn(true).when(predicate).test(instance3);
		doReturn(true).when(predicate).test(instance4);

		BackupFirstProcessingUnitInstanceComparator comparator = new BackupFirstProcessingUnitInstanceComparator();
		comparator.setPredicate(predicate);

		final List<ProcessingUnitInstance> list = new ArrayList<>();
		list.add(instance1);
		list.add(instance2);
		list.add(instance3);
		list.add(instance4);

		list.sort(comparator);

		assertSame(instance3, list.get(0));
		assertSame(instance4, list.get(1));
		assertSame(instance1, list.get(2));
		assertSame(instance2, list.get(3));
	}

}
