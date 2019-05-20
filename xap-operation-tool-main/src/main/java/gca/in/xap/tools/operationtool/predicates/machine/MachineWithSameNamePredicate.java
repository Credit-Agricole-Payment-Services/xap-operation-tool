package gca.in.xap.tools.operationtool.predicates.machine;

import lombok.Data;
import org.openspaces.admin.machine.Machine;

import java.util.function.Predicate;

@Data
public class MachineWithSameNamePredicate implements Predicate<Machine> {

	private final String hostNameOrAddress;

	@Override
	public boolean test(Machine m) {
		return m.getHostName().equals(hostNameOrAddress) || m.getHostAddress().equals(hostNameOrAddress);
	}

}
