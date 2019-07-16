package gca.in.xap.tools.operationtool.service.rebalance;

import lombok.Data;
import lombok.NonNull;

import java.util.*;

@Data
public class ZonesGroups {

	List<ZonesGroup> groups;

	public ZonesGroups() {
		this(new ArrayList<>());
	}

	public ZonesGroups(@NonNull List<ZonesGroup> groups) {
		super();
		this.groups = groups;
	}

	public void addNewGroup(Collection<String> zonesGroup) {
		if (zonesGroup.size() < 2) {
			throw new IllegalArgumentException("Zone Group " + zonesGroup + " should contain at least 2 values");
		}
		groups.add(new ZonesGroup(Collections.unmodifiableSortedSet(new TreeSet<>(zonesGroup))));
	}

}
