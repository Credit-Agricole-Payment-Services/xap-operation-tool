package gca.in.xap.tools.operationtool.commandoptions;

import org.openspaces.admin.GridComponent;
import picocli.CommandLine;

import java.util.List;
import java.util.function.Predicate;

public class MachinesFilterOptions<T extends GridComponent> {

	@CommandLine.Option(names = {"--machines-includes"}, split = ",", description = "List of names of the Machines to include. If you only want to act on a subset of the Machines, you can specify 1 or more machine name.")
	public List<String> machinesIncludes;

	@CommandLine.Option(names = {"--machines-excludes"}, split = ",", description = "List of names of the Machines to exclude. If you only want to act on a subset of the Machines, you can specify 1 or more machine name.")
	public List<String> machinesExcludes;

	public Predicate<T> toPredicate() {
		Predicate<T> includePredicate;
		if (machinesIncludes != null) {
			includePredicate = gridComponent -> machinesIncludes.contains(gridComponent.getMachine().getHostName()) || machinesIncludes.contains(gridComponent.getMachine().getHostAddress());
		} else {
			includePredicate = value -> true;
		}
		if (machinesExcludes != null) {
			return gridComponent -> !machinesExcludes.contains(gridComponent.getMachine().getHostName()) && !machinesExcludes.contains(gridComponent.getMachine().getHostAddress()) && includePredicate.test(gridComponent);
		} else {
			return includePredicate;
		}

	}

}
