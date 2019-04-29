package gca.in.xap.tools.operationtool.predicates.machine;

import lombok.Data;
import org.openspaces.admin.machine.Machine;

import java.util.function.Predicate;

@Data
class DifferentMachinePredicate implements Predicate<Machine> {

	private final Machine machine;

	@Override
	public boolean test(Machine m) {
		return !m.equals(machine);
	}

}
