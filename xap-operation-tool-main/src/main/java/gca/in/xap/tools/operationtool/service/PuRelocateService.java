package gca.in.xap.tools.operationtool.service;

import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.util.function.Predicate;
import java.util.stream.Stream;

public interface PuRelocateService {

	void relocatePuInstance(ProcessingUnitInstance puInstance, Predicate<Machine> machinePredicate, boolean await);

	Stream<GridServiceContainer> findBestContainersToRelocate(ProcessingUnitInstance puInstance, Predicate<Machine> machinePredicate);

	GridServiceContainer findBestContainerToRelocate(ProcessingUnitInstance puInstance, Predicate<Machine> machinePredicate);

}
