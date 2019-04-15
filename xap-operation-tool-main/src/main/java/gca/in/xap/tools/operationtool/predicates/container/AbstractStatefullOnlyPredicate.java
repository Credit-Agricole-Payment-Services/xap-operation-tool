package gca.in.xap.tools.operationtool.predicates.container;

import gca.in.xap.tools.operationtool.predicates.pu.PredicatesFactory;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.util.Arrays;
import java.util.function.Predicate;

public abstract class AbstractStatefullOnlyPredicate implements Predicate<GridServiceContainer> {

	@Override
	public boolean test(GridServiceContainer gsc) {
		ProcessingUnitInstance[] processingUnitInstances = gsc.getProcessingUnitInstances();
		// if the GSC is not running any PU, then we do not want to restart it
		if (processingUnitInstances.length == 0) {
			return false;
		}
		// if the GSC is running an stateful Primary PU, then we do not want to restart it
		// else, it means that we are only running backup PU(s) in this GSC
		final boolean containsStatefulBackupPU = Arrays.stream(processingUnitInstances).anyMatch(PredicatesFactory.containsStatefulBackupPUPredicate());
		final boolean containsStatefulPrimaryPU = Arrays.stream(processingUnitInstances).anyMatch(PredicatesFactory.containsStatefulPrimaryPUPredicate());
		final String gscId = gsc.getId();
		return this.test(gscId, containsStatefulBackupPU, containsStatefulPrimaryPU);
	}

	protected abstract boolean test(String gscId, boolean containsStatefulBackupPU, boolean containsStatefulPrimaryPU);

}
