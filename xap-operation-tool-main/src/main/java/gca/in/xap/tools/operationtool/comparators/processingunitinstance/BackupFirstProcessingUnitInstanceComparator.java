package gca.in.xap.tools.operationtool.comparators.processingunitinstance;

import gca.in.xap.tools.operationtool.predicates.pu.IsBackupStatefulProcessingUnitPredicate;
import lombok.Setter;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.util.Comparator;

public class BackupFirstProcessingUnitInstanceComparator implements Comparator<ProcessingUnitInstance> {

	@Setter
	private IsBackupStatefulProcessingUnitPredicate predicate = new IsBackupStatefulProcessingUnitPredicate();

	@Override
	public int compare(ProcessingUnitInstance o1, ProcessingUnitInstance o2) {
		final boolean o1_backup = predicate.test(o1);
		final boolean o2_backup = predicate.test(o2);
		if (o1_backup == o2_backup) {
			return 0;
		}
		if (o2_backup) {
			return 1;
		} else {
			return -1;
		}
	}

}
