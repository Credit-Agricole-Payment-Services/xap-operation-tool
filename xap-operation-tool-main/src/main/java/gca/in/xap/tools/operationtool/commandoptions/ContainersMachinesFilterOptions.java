package gca.in.xap.tools.operationtool.commandoptions;

import org.openspaces.admin.gsc.GridServiceContainer;
import picocli.CommandLine;

import java.util.List;
import java.util.function.Predicate;

public class ContainersMachinesFilterOptions {

	@CommandLine.Option(names = {"--machinesIncludes"}, description = "List of names of the Machines to include. If you only want to act on a subset of the Machines, you can specify 1 or more machine name.")
	public List<String> machinesIncludes;

	@CommandLine.Option(names = {"--machinesExcludes"}, description = "List of names of the Machines to exclude. If you only want to act on a subset of the Machines, you can specify 1 or more machine name.")
	public List<String> machinesExcludes;

	public Predicate<GridServiceContainer> toPredicate() {
		Predicate<GridServiceContainer> includePredicate;
		if (machinesIncludes != null) {
			includePredicate = gsc -> machinesIncludes.contains(gsc.getMachine().getHostName()) || machinesIncludes.contains(gsc.getMachine().getHostAddress());
		} else {
			includePredicate = value -> true;
		}
		if (machinesExcludes != null) {
			return gsc -> !machinesExcludes.contains(gsc.getMachine().getHostName()) && !machinesExcludes.contains(gsc.getMachine().getHostAddress()) && includePredicate.test(gsc);
		} else {
			return includePredicate;
		}

	}

}
