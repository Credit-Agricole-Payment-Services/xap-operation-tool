package gca.in.xap.tools.operationtool.predicates.machine;

import org.openspaces.admin.machine.Machine;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

@Component
public class MachinePredicateFactory {

	public Predicate<Machine> createMachinePredicate(String hostNameOrAddress) {
		return machine -> machine.getHostName().equals(hostNameOrAddress) || machine.getHostAddress().equals(hostNameOrAddress);
	}

	public Predicate<Machine> createSameMachinePredicate(Machine machine) {
		return m -> m.equals(machine);
	}

	public Predicate<Machine> createDifferentMachinePredicate(Machine machine) {
		return m -> !m.equals(machine);
	}

}
