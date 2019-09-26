package gca.in.xap.tools.operationtool.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.openspaces.admin.vm.VirtualMachine;

import java.util.Comparator;

@Data
@EqualsAndHashCode
@AllArgsConstructor
public class GlobalProcessId implements Comparable<GlobalProcessId> {

	public static GlobalProcessId toProcessIdentifier(VirtualMachine jvm) {
		return new GlobalProcessId(jvm.getMachine().getHostName(), jvm.getDetails().getPid());
	}

	@NonNull
	private final String hostName;

	@NonNull
	private final Long pid;

	@Override
	public int compareTo(GlobalProcessId o) {
		return Comparator
				.comparing(GlobalProcessId::getHostName)
				.thenComparing(GlobalProcessId::getPid)
				.compare(this, o);
	}
}
