package gca.in.xap.tools.operationtool.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import java.util.Comparator;

@Data
@EqualsAndHashCode
@AllArgsConstructor
public class GlobalAgentId implements Comparable<GlobalAgentId> {

	@NonNull
	private final String hostName;

	@NonNull
	private final Integer agentId;

	@Override
	public int compareTo(GlobalAgentId o) {
		return Comparator
				.comparing(GlobalAgentId::getHostName)
				.thenComparing(GlobalAgentId::getAgentId)
				.compare(this, o);
	}
}
