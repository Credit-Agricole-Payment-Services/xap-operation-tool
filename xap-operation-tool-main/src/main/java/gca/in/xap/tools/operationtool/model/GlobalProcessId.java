package gca.in.xap.tools.operationtool.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.Comparator;

@Data
@EqualsAndHashCode
@AllArgsConstructor
public class GlobalProcessId implements Comparable<GlobalProcessId> {

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
