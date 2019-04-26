package gca.in.xap.tools.operationtool.service;

import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.util.function.Predicate;

public interface PuRelocateService {

	void relocatePuInstance(ProcessingUnitInstance puInstance, Predicate<Machine> machinePredicate);

}
