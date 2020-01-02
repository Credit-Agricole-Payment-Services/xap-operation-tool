package gca.in.xap.tools.operationtool.service.rebalance;

import gca.in.xap.tools.operationtool.comparators.processingunitinstance.BackupFirstProcessingUnitInstanceComparator;
import gca.in.xap.tools.operationtool.predicates.machine.MachineWithSameNamePredicate;
import gca.in.xap.tools.operationtool.service.IdExtractor;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.userinput.UserConfirmationService;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static gca.in.xap.tools.operationtool.service.rebalance.MinAndMax.findMinAndMax;

@Slf4j
@Component
public class DefaultRebalanceProcessingUnitService implements RebalanceProcessingUnitService {

	private static Set<String> extractIds(Collection<ProcessingUnitInstance> processingUnitInstances) {
		return new LinkedHashSet<>(processingUnitInstances.stream().map(ProcessingUnitInstance::getId).collect(Collectors.toSet()));
	}

	@Autowired
	@Lazy
	@Setter
	private XapService xapService;

	@Autowired
	@Setter
	private UserConfirmationService userConfirmationService;

	@Autowired
	@Setter
	private ProcessingUnitInstanceStateSnapshotService processingUnitInstanceStateSnapshotService;

	@Autowired
	@Setter
	private IdExtractor idExtractor;

	@Autowired
	@Setter
	private RelocatePuBalanceFixingAction relocatePuBalanceFixingAction;

	@Autowired
	@Setter
	private DemotePuBalanceFixingAction demotePuBalanceFixingAction;

	@Override
	public void rebalanceProcessingUnit(
			@NonNull String processingUnitName,
			boolean onceOnly,
			@Nullable ZonesGroups zonesGroups,
			Duration demoteMaxSuspendDuration
	) {
		log.debug("processingUnitName = {}", processingUnitName);
		ProcessingUnit processingUnit = xapService.findProcessingUnitByName(processingUnitName);
		if (processingUnit == null) {
			throw new IllegalStateException("ProcessingUnit with name " + processingUnitName + " was not found");
		}
		log.info("Rebalancing Processing Unit {} if needed ...", processingUnit.getName());

		final ProcessingUnitInstanceStateSnapshot initialStateSnapshot = processingUnitInstanceStateSnapshotService.takeSnapshot(processingUnit);
		log.info("initialStateSnapshot = {}", initialStateSnapshot.toJsonWithoutZeros());

		ProcessingUnitInstanceStateSnapshot latestStateSnapshot = initialStateSnapshot;
		boolean lastIterationRebalanced;
		do {
			lastIterationRebalanced = doRebalance(latestStateSnapshot, zonesGroups, demoteMaxSuspendDuration);

			if (lastIterationRebalanced) {
				final ProcessingUnitInstanceStateSnapshot processingUnitInstanceStateSnapshotAfter = processingUnitInstanceStateSnapshotService.takeSnapshot(processingUnit);
				log.info("processingUnitInstanceStateSnapshotAfter = {}", processingUnitInstanceStateSnapshotAfter.toJsonWithoutZeros());
				latestStateSnapshot = processingUnitInstanceStateSnapshotAfter;
			}
		} while (!onceOnly && lastIterationRebalanced);
	}

	private boolean doRebalance(
			@NonNull ProcessingUnitInstanceStateSnapshot stateSnapshotBefore,
			@Nullable ZonesGroups zonesGroups,
			Duration demoteMaxSuspendDuration
	) {
		boolean rebalanced = rebalanceByBreakDownOnEachPartition(stateSnapshotBefore, zonesGroups, demoteMaxSuspendDuration);
		if (rebalanced) {
			return true;
		}
		final Predicate<ProcessingUnitInstance> matchingProcessingUnitPredicateForBreakdown = processingUnitInstance -> true;
		rebalanced = rebalanceByBreakDown(relocatePuBalanceFixingAction, "total instances", stateSnapshotBefore.processingUnitInstanceRepartitionSnapshot.actualTotalCounts, stateSnapshotBefore, zonesGroups, matchingProcessingUnitPredicateForBreakdown, demoteMaxSuspendDuration);
		if (rebalanced) {
			return true;
		}
		rebalanced = rebalanceByBreakDown(demotePuBalanceFixingAction, "total primary instances", stateSnapshotBefore.processingUnitInstanceRepartitionSnapshot.actualPrimaryCounts, stateSnapshotBefore, zonesGroups, matchingProcessingUnitPredicateForBreakdown, demoteMaxSuspendDuration);
		if (rebalanced) {
			return true;
		}
		rebalanced = rebalanceByBreakDown(relocatePuBalanceFixingAction, "total backup instances", stateSnapshotBefore.processingUnitInstanceRepartitionSnapshot.actualBackupCounts, stateSnapshotBefore, zonesGroups, matchingProcessingUnitPredicateForBreakdown, demoteMaxSuspendDuration);
		if (rebalanced) {
			return true;
		}
		return rebalanced;
	}

	private boolean rebalanceByBreakDownOnEachPartition(
			@NonNull ProcessingUnitInstanceStateSnapshot processingUnitInstanceStateSnapshotBefore,
			@Nullable ZonesGroups zonesGroups,
			Duration demoteMaxSuspendDuration
	) {
		for (Map.Entry<Integer, ProcessingUnitInstanceRepartitionSnapshot> entry : processingUnitInstanceStateSnapshotBefore.processingUnitInstanceRepartitionSnapshotPerPartition.entrySet()) {
			final Integer partitionId = entry.getKey();
			final ProcessingUnitInstanceRepartitionSnapshot snapshotForPartition = entry.getValue();
			//
			final int partitionIndex = partitionId + 1;
			final String breakdownDescription = "Partition #" + partitionIndex;
			//
			final Predicate<ProcessingUnitInstance> matchingProcessingUnitPredicateForBreakdown = processingUnitInstance -> processingUnitInstance.getPartition().getPartitionId() == partitionId;
			boolean rebalanceDone = rebalanceByBreakDown(relocatePuBalanceFixingAction, breakdownDescription + " instances", snapshotForPartition.actualTotalCounts, processingUnitInstanceStateSnapshotBefore, zonesGroups, matchingProcessingUnitPredicateForBreakdown, demoteMaxSuspendDuration);
			if (rebalanceDone) {
				log.info("Partition Id {} has been relocated", partitionId);
				return true;
			}
			rebalanceDone = rebalanceByBreakDown(demotePuBalanceFixingAction, breakdownDescription + " primary instances", snapshotForPartition.actualPrimaryCounts, processingUnitInstanceStateSnapshotBefore, zonesGroups, matchingProcessingUnitPredicateForBreakdown, demoteMaxSuspendDuration);
			if (rebalanceDone) {
				log.info("Partition Id {} has been relocated", partitionId);
				return true;
			}
			rebalanceDone = rebalanceByBreakDown(relocatePuBalanceFixingAction, breakdownDescription + " backup instances", snapshotForPartition.actualBackupCounts, processingUnitInstanceStateSnapshotBefore, zonesGroups, matchingProcessingUnitPredicateForBreakdown, demoteMaxSuspendDuration);
			if (rebalanceDone) {
				log.info("Partition Id {} has been relocated", partitionId);
				return true;
			}
		}
		return false;
	}

	private boolean rebalanceByBreakDown(
			BalanceFixingAction balanceFixingAction,
			@NonNull String breakdownDescription,
			@NonNull ProcessingUnitInstanceBreakdownSnapshot breakdown,
			@NonNull ProcessingUnitInstanceStateSnapshot processingUnitInstanceStateSnapshotBefore,
			@Nullable ZonesGroups zonesGroups,
			@NonNull Predicate<ProcessingUnitInstance> matchingProcessingUnitPredicateForBreakdown,
			@NonNull Duration demoteMaxSuspendDuration
	) {
		if (zonesGroups != null) {
			for (ZonesGroup zonesGroup : zonesGroups.getGroups()) {
				final MinAndMax<String> minAndMaxByZone = findMinAndMax(breakdown.countByZone, s -> zonesGroup.getZones().contains(s));
				final boolean needsRebalancedByZone = minAndMaxByZone != null && minAndMaxByZone.needsRebalancing();
				log.info("breakdownDescription = {}, zonesGroup = {}, zonesGroup.getZones().size() = {}, breakdown.countByZone = {}, minAndMaxByZone = {}, needsRebalancedByZone = {}",
						breakdownDescription,
						zonesGroup,
						zonesGroup.getZones().size(),
						breakdown.countByZone,
						minAndMaxByZone,
						needsRebalancedByZone);
				if (needsRebalancedByZone) {
					rebalanceByZone(balanceFixingAction, processingUnitInstanceStateSnapshotBefore.allProcessingUnitInstances, minAndMaxByZone, matchingProcessingUnitPredicateForBreakdown, demoteMaxSuspendDuration);
					return true;
				}
			}
		}
		MinAndMax<String> minAndMaxByMachine = findMinAndMax(breakdown.countByMachine);
		boolean needsRebalancedByMachine = minAndMaxByMachine != null && minAndMaxByMachine.needsRebalancing();
		if (needsRebalancedByMachine) {
			rebalanceByMachine(balanceFixingAction, processingUnitInstanceStateSnapshotBefore.allProcessingUnitInstances, minAndMaxByMachine, matchingProcessingUnitPredicateForBreakdown, demoteMaxSuspendDuration);
			return true;
		}
		MinAndMax<String> minAndMaxByGSC = findMinAndMax(breakdown.countByGSC);
		boolean needsRebalancedByGSC = minAndMaxByGSC != null && minAndMaxByGSC.needsRebalancing();
		if (needsRebalancedByGSC) {
			rebalanceByGSC(balanceFixingAction, processingUnitInstanceStateSnapshotBefore.allProcessingUnitInstances, minAndMaxByGSC, matchingProcessingUnitPredicateForBreakdown, demoteMaxSuspendDuration);
			return true;
		}
		log.info("Does not need to relocate any PU Instance for distribution of {}", breakdownDescription);
		return false;
	}

	private void rebalanceByZone(
			BalanceFixingAction balanceFixingAction,
			@NonNull ProcessingUnitInstance[] allProcessingUnitInstances,
			@NonNull MinAndMax<String> minAndMaxByZone,
			@NonNull Predicate<ProcessingUnitInstance> matchingProcessingUnitPredicateForBreakdown,
			@NonNull Duration demoteMaxSuspendDuration
	) {
		log.info("Rebalancing ProcessingUnit by Zone : minAndMaxByZone = {}", minAndMaxByZone);

		/**
		 Set<Integer> partitionIdsOnZoneWithMinCount = Arrays.stream(processingUnitInstances)
		 .filter(processingUnitInstance -> processingUnitInstance.getGridServiceContainer().getExactZones().getZones().contains(minAndMaxByZone.getMin().getKey()))
		 .map(processingUnitInstance -> processingUnitInstance.getPartition().getPartitionId())
		 .collect(Collectors.toSet());

		 log.info("partitionIdsOnZoneWithMinCount = {}", partitionIdsOnZoneWithMinCount);
		 */

		List<ProcessingUnitInstance> candidateProcessingUnitInstancesToRelocate = Arrays.stream(allProcessingUnitInstances)
				.filter(matchingProcessingUnitPredicateForBreakdown)
				.filter(processingUnitInstance -> processingUnitInstance.getGridServiceContainer().getExactZones().getZones().contains(minAndMaxByZone.getBestKeyOfMax()))
				//.filter(processingUnitInstance -> !partitionIdsOnZoneWithMinCount.contains(processingUnitInstance.getPartition().getPartitionId()))
				.sorted(Collections.reverseOrder(new BackupFirstProcessingUnitInstanceComparator()))
				.collect(Collectors.toList());

		log.info("candidateProcessingUnitInstancesToRelocate = {}", idExtractor.extractProcessingUnitsNamesAndDescription(candidateProcessingUnitInstancesToRelocate.toArray(new ProcessingUnitInstance[0])));

		final ProcessingUnitInstance processingUnitInstanceToRelocate = candidateProcessingUnitInstancesToRelocate.get(0);
		final Predicate<Machine> targetMachinePredicate = machine -> true;

		balanceFixingAction.doFixBalance(processingUnitInstanceToRelocate, minAndMaxByZone, targetMachinePredicate, BreakdownAxis.ZONE, demoteMaxSuspendDuration);
	}

	private void rebalanceByMachine(
			BalanceFixingAction balanceFixingAction,
			@NonNull ProcessingUnitInstance[] allProcessingUnitInstances,
			@NonNull MinAndMax<String> minAndMaxByMachine,
			@NonNull Predicate<ProcessingUnitInstance> matchingProcessingUnitPredicateForBreakdown,
			@NonNull Duration demoteMaxSuspendDuration
	) {
		log.info("Rebalancing ProcessingUnit by Machine : minAndMaxByMachine = {}", minAndMaxByMachine);

		Set<Integer> partitionIdsOnMachineWithMinCount = Arrays.stream(allProcessingUnitInstances)
				.filter(processingUnitInstance -> processingUnitInstance.getGridServiceContainer().getMachine().getHostName().equals(minAndMaxByMachine.getBestKeyOfMin()))
				.map(processingUnitInstance -> processingUnitInstance.getPartition().getPartitionId())
				.collect(Collectors.toSet());

		log.info("partitionIdsOnMachineWithMinCount = {}", partitionIdsOnMachineWithMinCount);

		List<ProcessingUnitInstance> candidateProcessingUnitInstancesToRelocate = Arrays.stream(allProcessingUnitInstances)
				.filter(matchingProcessingUnitPredicateForBreakdown)
				.filter(processingUnitInstance -> processingUnitInstance.getGridServiceContainer().getMachine().getHostName().equals(minAndMaxByMachine.getBestKeyOfMax()))
				.filter(processingUnitInstance -> !partitionIdsOnMachineWithMinCount.contains(processingUnitInstance.getPartition().getPartitionId()))
				.sorted(Collections.reverseOrder(new BackupFirstProcessingUnitInstanceComparator()))
				.collect(Collectors.toList());

		log.info("candidateProcessingUnitInstancesToRelocate = {}", extractIds(candidateProcessingUnitInstancesToRelocate));

		if (!candidateProcessingUnitInstancesToRelocate.isEmpty()) {
			final ProcessingUnitInstance processingUnitInstanceToRelocate = candidateProcessingUnitInstancesToRelocate.get(0);
			final Predicate<Machine> targetMachinePredicate = new MachineWithSameNamePredicate(minAndMaxByMachine.getBestKeyOfMin());
			balanceFixingAction.doFixBalance(processingUnitInstanceToRelocate, minAndMaxByMachine, targetMachinePredicate, BreakdownAxis.MACHINE, demoteMaxSuspendDuration);
		} else {
			log.warn("Did not found any candidate ProcessingUnitInstance to relocate according to criterias");
		}
	}

	private void rebalanceByGSC(
			@NonNull BalanceFixingAction balanceFixingAction,
			@NonNull ProcessingUnitInstance[] allProcessingUnitInstances,
			@NonNull MinAndMax<String> minAndMaxByGSC,
			@NonNull Predicate<ProcessingUnitInstance> matchingProcessingUnitPredicateForBreakdown,
			@NonNull Duration demoteMaxSuspendDuration
	) {
		log.info("Rebalancing ProcessingUnit by GSC ...");

		Set<Integer> partitionIdsOnMachineWithMinCount = Arrays.stream(allProcessingUnitInstances)
				.filter(matchingProcessingUnitPredicateForBreakdown)
				.filter(processingUnitInstance -> processingUnitInstance.getGridServiceContainer().getId().equals(minAndMaxByGSC.getBestKeyOfMin()))
				.map(processingUnitInstance -> processingUnitInstance.getPartition().getPartitionId())
				.collect(Collectors.toSet());

		log.info("partitionIdsOnMachineWithMinCount = {}", partitionIdsOnMachineWithMinCount);

		List<ProcessingUnitInstance> candidateProcessingUnitInstancesToRelocate = Arrays.stream(allProcessingUnitInstances)
				.filter(processingUnitInstance -> processingUnitInstance.getGridServiceContainer().getId().equals(minAndMaxByGSC.getBestKeyOfMax()))
				.filter(processingUnitInstance -> !partitionIdsOnMachineWithMinCount.contains(processingUnitInstance.getPartition().getPartitionId()))
				.sorted(Collections.reverseOrder(new BackupFirstProcessingUnitInstanceComparator()))
				.collect(Collectors.toList());

		log.info("candidateProcessingUnitInstancesToRelocate = {}", extractIds(candidateProcessingUnitInstancesToRelocate));

		final ProcessingUnitInstance processingUnitInstanceToRelocate = candidateProcessingUnitInstancesToRelocate.get(0);
		final Predicate<Machine> targetMachinePredicate = machine -> true;

		balanceFixingAction.doFixBalance(processingUnitInstanceToRelocate, minAndMaxByGSC, targetMachinePredicate, BreakdownAxis.CONTAINER, demoteMaxSuspendDuration);
	}

}

