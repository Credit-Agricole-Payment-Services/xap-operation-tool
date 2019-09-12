package gca.in.xap.tools.operationtool.service;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.util.function.Predicate;

public interface PuRelocateService {

	void relocatePuInstance(
			ProcessingUnitInstance puInstance,
			Predicate<Machine> machinePredicate,
			boolean await,
			boolean demoteFirst
	);

	GridServiceContainer[] findBestContainersToRelocate(
			ProcessingUnit processingUnit,
			Predicate<Machine> machinePredicate,
			Predicate<GridServiceContainer> gridServiceContainerPredicate
	);

	GridServiceContainer findBestContainerToRelocate(
			ProcessingUnit processingUnit,
			Predicate<Machine> machinePredicate,
			Predicate<GridServiceContainer> gridServiceContainerPredicate
	);

}
