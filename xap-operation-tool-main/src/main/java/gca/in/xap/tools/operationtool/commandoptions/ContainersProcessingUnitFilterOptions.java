package gca.in.xap.tools.operationtool.commandoptions;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import picocli.CommandLine;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class ContainersProcessingUnitFilterOptions {

	@CommandLine.Option(names = {"--puIncludes"}, split=",", description = "List of names of the Processing Units to include. If you only want to act on a subset of the Processing Units, you can specify 1 or more processing units to include in this deployment.")
	public List<String> processingUnitsIncludes;

	@CommandLine.Option(names = {"--puExcludes"}, split=",", description = "List of names of the Processing Units to exclude. If you only want to act on a subset of the Processing Units, you can specify 1 or more processing units to exclude from this deployment.")
	public List<String> processingUnitsExcludes;

	public Predicate<GridServiceContainer> toPredicate() {
		Predicate<GridServiceContainer> includePredicate;
		if (processingUnitsIncludes != null) {
			includePredicate = gsc -> isRunningOneOfProcessingUnits(gsc, processingUnitsIncludes);
		} else {
			includePredicate = value -> true;
		}
		if (processingUnitsExcludes != null) {
			return gsc -> isRunningNoneOfProcessingUnits(gsc, processingUnitsExcludes) && includePredicate.test(gsc);
		} else {
			return includePredicate;
		}

	}

	private static boolean isRunningOneOfProcessingUnits(GridServiceContainer gsc, Collection<String> puNames) {
		ProcessingUnitInstance[] processingUnitInstances = gsc.getProcessingUnitInstances();
		for (ProcessingUnitInstance puInstance : processingUnitInstances) {
			String puName = puInstance.getProcessingUnit().getName();
			if (puNames.contains(puName)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isRunningNoneOfProcessingUnits(GridServiceContainer gsc, Collection<String> puNames) {
		ProcessingUnitInstance[] processingUnitInstances = gsc.getProcessingUnitInstances();
		for (ProcessingUnitInstance puInstance : processingUnitInstances) {
			String puName = puInstance.getProcessingUnit().getName();
			if (puNames.contains(puName)) {
				return false;
			}
		}
		return true;
	}

}
