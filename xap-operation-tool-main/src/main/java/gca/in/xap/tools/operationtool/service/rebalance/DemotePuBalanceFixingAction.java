package gca.in.xap.tools.operationtool.service.rebalance;

import com.gigaspaces.cluster.activeelection.SpaceMode;
import gca.in.xap.tools.operationtool.service.DemoteService;
import gca.in.xap.tools.operationtool.userinput.UserConfirmationService;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnitPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Predicate;

@Slf4j
@Component
public class DemotePuBalanceFixingAction implements BalanceFixingAction {

	@Autowired
	@Setter
	private UserConfirmationService userConfirmationService;

	@Autowired
	@Setter
	private DemoteService demoteService;

	@Override
	public void doFixBalance(
			@NonNull ProcessingUnitInstance processingUnitInstanceToRelocate,
			@NonNull MinAndMax<String> minAndMax,
			@NonNull Predicate<Machine> targetMachinePredicate,
			@NonNull BreakdownAxis breakdownAxis,
			@NonNull Duration demoteMaxSuspendDuration
	) {
		ProcessingUnitPartition processingUnitPartition = processingUnitInstanceToRelocate.getPartition();
		final int partitionIndex = processingUnitPartition.getPartitionId() + 1;
		final String primaryOrBackupIndicator = (processingUnitInstanceToRelocate.getSpaceInstance() != null && processingUnitInstanceToRelocate.getSpaceInstance().getMode() == SpaceMode.PRIMARY) ? "P" : "B";

		log.warn("Will demote instance of Processing Unit Instance {} (Partition #{} ({})) from {} {} to {} {}",
				processingUnitInstanceToRelocate.getId(),
				partitionIndex,
				primaryOrBackupIndicator,
				breakdownAxis, minAndMax.getBestKeyOfMax(),
				breakdownAxis, minAndMax.getBestKeyOfMin()
		);
		userConfirmationService.askConfirmationAndWait();

		demoteService.demotePrimarySpaceInstances(processingUnitInstanceToRelocate, demoteMaxSuspendDuration);
	}

}
