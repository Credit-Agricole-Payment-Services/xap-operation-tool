package gca.in.xap.tools.operationtool.service.rebalance;

import lombok.NonNull;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.time.Duration;
import java.util.function.Predicate;

public interface BalanceFixingAction {

	void doFixBalance(
			@NonNull ProcessingUnitInstance processingUnitInstanceToRelocate,
			@NonNull MinAndMax<String> minAndMax,
			@NonNull Predicate<Machine> targetMachinePredicate,
			@NonNull BreakdownAxis breakdownAxis,
			@NonNull Duration demoteMaxSuspendDuration
	);

}
