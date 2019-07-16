package gca.in.xap.tools.operationtool.service.rebalance;

import com.gigaspaces.cluster.activeelection.SpaceMode;
import gca.in.xap.tools.operationtool.comparators.processingunitinstance.BackupFirstProcessingUnitInstanceComparator;
import gca.in.xap.tools.operationtool.predicates.machine.MachineWithSameNamePredicate;
import gca.in.xap.tools.operationtool.service.IdExtractor;
import gca.in.xap.tools.operationtool.service.PuRelocateService;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.userinput.UserConfirmationService;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnitPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
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
	@Setter
	private PuRelocateService puRelocateService;

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

	@Override
	public void rebalanceProcessingUnit(
			@NonNull String processingUnitName,
			boolean onceOnly,
			@Nullable ZonesGroups zonesGroups
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
			lastIterationRebalanced = doRebalance(latestStateSnapshot, zonesGroups);

			if (lastIterationRebalanced) {
				final ProcessingUnitInstanceStateSnapshot processingUnitInstanceStateSnapshotAfter = processingUnitInstanceStateSnapshotService.takeSnapshot(processingUnit);
				log.info("processingUnitInstanceStateSnapshotAfter = {}", processingUnitInstanceStateSnapshotAfter.toJsonWithoutZeros());
				latestStateSnapshot = processingUnitInstanceStateSnapshotAfter;
			}
		} while (!onceOnly && lastIterationRebalanced);
	}

	private boolean doRebalance(
			@NonNull ProcessingUnitInstanceStateSnapshot stateSnapshotBefore,
			@Nullable ZonesGroups zonesGroups
	) {
		boolean rebalanced = rebalanceByBreakDownOnEachPartition(stateSnapshotBefore, zonesGroups);
		if (rebalanced) {
			return true;
		}
		final Predicate<ProcessingUnitInstance> matchingProcessingUnitPredicateForBreakdown = processingUnitInstance -> true;
		rebalanced = rebalanceByBreakDown("total instances", stateSnapshotBefore.processingUnitInstanceRepartitionSnapshot.actualTotalCounts, stateSnapshotBefore, zonesGroups, matchingProcessingUnitPredicateForBreakdown);
		if (rebalanced) {
			return true;
		}
		return false;
	}

	private boolean rebalanceByBreakDownOnEachPartition(
			@NonNull ProcessingUnitInstanceStateSnapshot processingUnitInstanceStateSnapshotBefore,
			@Nullable ZonesGroups zonesGroups
	) {
		for (Map.Entry<Integer, ProcessingUnitInstanceRepartitionSnapshot> entry : processingUnitInstanceStateSnapshotBefore.processingUnitInstanceRepartitionSnapshotPerPartition.entrySet()) {
			final Integer partitionId = entry.getKey();
			final ProcessingUnitInstanceRepartitionSnapshot snapshotForPartition = entry.getValue();
			//
			final int partitionIndex = partitionId + 1;
			final String breakdownDescription = "Partition #" + partitionIndex;
			//
			final Predicate<ProcessingUnitInstance> matchingProcessingUnitPredicateForBreakdown = processingUnitInstance -> processingUnitInstance.getPartition().getPartitionId() == partitionId;
			boolean rebalanceDone = rebalanceByBreakDown(breakdownDescription, snapshotForPartition.actualTotalCounts, processingUnitInstanceStateSnapshotBefore, zonesGroups, matchingProcessingUnitPredicateForBreakdown);
			if (rebalanceDone) {
				log.info("Partition Id {} has been relocated", partitionId);
				return true;
			}
		}
		return false;
	}

	private boolean rebalanceByBreakDown(
			@NonNull String breakdownDescription,
			@NonNull ProcessingUnitInstanceBreakdownSnapshot breakdown,
			@NonNull ProcessingUnitInstanceStateSnapshot processingUnitInstanceStateSnapshotBefore,
			@Nullable ZonesGroups zonesGroups,
			@NonNull Predicate<ProcessingUnitInstance> matchingProcessingUnitPredicateForBreakdown
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
					rebalanceByZone(processingUnitInstanceStateSnapshotBefore.allProcessingUnitInstances, minAndMaxByZone, matchingProcessingUnitPredicateForBreakdown);
					return true;
				}
			}
		}
		MinAndMax<String> minAndMaxByMachine = findMinAndMax(breakdown.countByMachine);
		boolean needsRebalancedByMachine = minAndMaxByMachine != null && minAndMaxByMachine.needsRebalancing();
		if (needsRebalancedByMachine) {
			rebalanceByMachine(processingUnitInstanceStateSnapshotBefore.allProcessingUnitInstances, minAndMaxByMachine, matchingProcessingUnitPredicateForBreakdown);
			return true;
		}
		MinAndMax<String> minAndMaxByGSC = findMinAndMax(breakdown.countByGSC);
		boolean needsRebalancedByGSC = minAndMaxByGSC != null && minAndMaxByGSC.needsRebalancing();
		if (needsRebalancedByGSC) {
			rebalanceByGSC(processingUnitInstanceStateSnapshotBefore.allProcessingUnitInstances, minAndMaxByGSC, matchingProcessingUnitPredicateForBreakdown);
			return true;
		}
		log.info("Does not need to relocate any PU Instance for distribution of {}", breakdownDescription);
		return false;
	}

	private void doRelocate(
			@NonNull ProcessingUnitInstance processingUnitInstanceToRelocate,
			@NonNull MinAndMax<String> minAndMax,
			@NonNull Predicate<Machine> targetMachinePredicate,
			@NonNull BreakdownAxis breakdownAxis
	) {
		ProcessingUnitPartition processingUnitPartition = processingUnitInstanceToRelocate.getPartition();
		final int partitionIndex = processingUnitPartition.getPartitionId() + 1;
		final String primaryOrBackupIndicator = (processingUnitInstanceToRelocate.getSpaceInstance() != null && processingUnitInstanceToRelocate.getSpaceInstance().getMode() == SpaceMode.PRIMARY) ? "P" : "B";

		log.warn("Will relocate instance of Processing Unit Instance {} (Partition #{} ({})) from {} {} to {} {}",
				processingUnitInstanceToRelocate.getId(),
				partitionIndex,
				primaryOrBackupIndicator,
				breakdownAxis, minAndMax.getBestKeyOfMax(),
				breakdownAxis, minAndMax.getBestKeyOfMin()
		);
		userConfirmationService.askConfirmationAndWait();

		//
		puRelocateService.relocatePuInstance(processingUnitInstanceToRelocate, targetMachinePredicate, true);
	}

	private void rebalanceByZone(
			@NonNull ProcessingUnitInstance[] allProcessingUnitInstances,
			@NonNull MinAndMax<String> minAndMaxByZone,
			@NonNull Predicate<ProcessingUnitInstance> matchingProcessingUnitPredicateForBreakdown
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

		doRelocate(processingUnitInstanceToRelocate, minAndMaxByZone, targetMachinePredicate, BreakdownAxis.ZONE);
	}

	private void rebalanceByMachine(
			@NonNull ProcessingUnitInstance[] allProcessingUnitInstances,
			@NonNull MinAndMax<String> minAndMaxByMachine,
			@NonNull Predicate<ProcessingUnitInstance> matchingProcessingUnitPredicateForBreakdown
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

		final ProcessingUnitInstance processingUnitInstanceToRelocate = candidateProcessingUnitInstancesToRelocate.get(0);
		final Predicate<Machine> targetMachinePredicate = new MachineWithSameNamePredicate(minAndMaxByMachine.getBestKeyOfMin());

		doRelocate(processingUnitInstanceToRelocate, minAndMaxByMachine, targetMachinePredicate, BreakdownAxis.MACHINE);
	}

	private void rebalanceByGSC(
			@NonNull ProcessingUnitInstance[] allProcessingUnitInstances,
			@NonNull MinAndMax<String> minAndMaxByGSC,
			@NonNull Predicate<ProcessingUnitInstance> matchingProcessingUnitPredicateForBreakdown
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

		doRelocate(processingUnitInstanceToRelocate, minAndMaxByGSC, targetMachinePredicate, BreakdownAxis.CONTAINER);
	}

}

