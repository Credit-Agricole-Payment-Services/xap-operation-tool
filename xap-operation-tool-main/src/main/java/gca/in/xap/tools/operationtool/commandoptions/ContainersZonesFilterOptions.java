package gca.in.xap.tools.operationtool.commandoptions;

import org.openspaces.admin.gsc.GridServiceContainer;
import picocli.CommandLine;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class ContainersZonesFilterOptions {

	@CommandLine.Option(names = {"--zones-includes"}, split = ",", description = "List of names of the Zones to include. If you only want to act on a subset of the Zones, you can specify 1 or more zone name.")
	public List<String> zonesIncludes;

	@CommandLine.Option(names = {"--zones-excludes"}, split = ",", description = "List of names of the Zones to exclude. If you only want to act on a subset of the Zones, you can specify 1 or more zone name.")
	public List<String> zonesExcludes;

	public Predicate<GridServiceContainer> toPredicate() {
		Predicate<GridServiceContainer> includePredicate;
		if (zonesIncludes != null) {
			includePredicate = gsc -> matchesAtLeastOne(zonesIncludes, gsc.getExactZones().getZones());
		} else {
			includePredicate = value -> true;
		}
		if (zonesExcludes != null) {
			return gsc -> matchesNone(zonesExcludes, gsc.getExactZones().getZones()) && includePredicate.test(gsc);
		} else {
			return includePredicate;
		}
	}

	public boolean matchesAtLeastOne(Collection<String> filterValues, Set<String> values) {
		for (String filterValue : filterValues) {
			if (values.contains(filterValue)) {
				return true;
			}
		}
		return false;
	}

	public boolean matchesNone(Collection<String> filterValues, Set<String> values) {
		for (String filterValue : filterValues) {
			if (values.contains(filterValue)) {
				return false;
			}
		}
		return true;
	}

}
