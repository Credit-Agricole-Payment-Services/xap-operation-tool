package gca.in.xap.tools.operationtool.commandoptions;

import gca.in.xap.tools.operationtool.predicates.AndPredicate;
import org.openspaces.admin.gsc.GridServiceContainer;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.util.function.Predicate;

public class CommonContainerFilteringOptions {

	public static Predicate<GridServiceContainer> toPredicate(@Nullable CommonContainerFilteringOptions commonContainerFilteringOptions) {
		if (commonContainerFilteringOptions == null) {
			commonContainerFilteringOptions = new CommonContainerFilteringOptions();
		}
		return commonContainerFilteringOptions.toPredicate();
	}

	@CommandLine.ArgGroup(exclusive = false)
	private ContainersZonesFilterOptions containersZonesFilterOptions;

	@CommandLine.ArgGroup(exclusive = false)
	private MachinesFilterOptions<GridServiceContainer> machinesFilterOptions;

	@CommandLine.ArgGroup(exclusive = false)
	private ContainersProcessingUnitFilterOptions containersProcessingUnitFilterOptions;

	@CommandLine.ArgGroup(exclusive = false)
	private ContainersUptimeFilterOptions containersUptimeFilterOptions;

	private void initNullFields() {
		if (containersZonesFilterOptions == null) {
			containersZonesFilterOptions = new ContainersZonesFilterOptions();
		}
		if (machinesFilterOptions == null) {
			machinesFilterOptions = new MachinesFilterOptions<>();
		}
		if (containersProcessingUnitFilterOptions == null) {
			containersProcessingUnitFilterOptions = new ContainersProcessingUnitFilterOptions();
		}
		if (containersUptimeFilterOptions == null) {
			containersUptimeFilterOptions = new ContainersUptimeFilterOptions();
		}
	}

	public Predicate<GridServiceContainer> toPredicate() {
		initNullFields();
		return new AndPredicate<>(
				containersZonesFilterOptions.toPredicate(),
				machinesFilterOptions.toPredicate(),
				containersProcessingUnitFilterOptions.toPredicate(),
				containersUptimeFilterOptions.toPredicate()
		);
	}

}
