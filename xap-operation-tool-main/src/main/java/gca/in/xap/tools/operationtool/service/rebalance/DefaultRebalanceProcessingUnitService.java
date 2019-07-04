package gca.in.xap.tools.operationtool.service.rebalance;

import com.gigaspaces.cluster.activeelection.SpaceMode;
import com.google.common.util.concurrent.AtomicLongMap;
import gca.in.xap.tools.operationtool.comparators.processingunitinstance.BackupFirstProcessingUnitInstanceComparator;
import gca.in.xap.tools.operationtool.predicates.machine.MachineWithSameNamePredicate;
import gca.in.xap.tools.operationtool.predicates.pu.IsBackupStatefulProcessingUnitPredicate;
import gca.in.xap.tools.operationtool.predicates.pu.IsPrimaryStatefulProcessingUnitPredicate;
import gca.in.xap.tools.operationtool.service.PuRelocateService;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.userinput.UserConfirmationService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.pu.ProcessingUnitPartition;
import org.openspaces.admin.zone.config.ExactZonesConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static gca.in.xap.tools.operationtool.service.rebalance.MinAndMax.findMinAndMax;

@Slf4j
@Component
public class DefaultRebalanceProcessingUnitService implements RebalanceProcessingUnitService {

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

	@Override
	public void rebalanceProcessingUnit(String processingUnitName, boolean onceOnly) {
		log.debug("processingUnitName = {}", processingUnitName);
		ProcessingUnit processingUnit = xapService.findProcessingUnitByName(processingUnitName);
		if (processingUnit == null) {
			throw new IllegalStateException("ProcessingUnit with name " + processingUnitName + " was not found");
		}
		log.info("Rebalancing Processing Unit {} if needed ...", processingUnit.getName());

		final ProcessingUnitInstanceStateSnapshot initialStateSnapshot = takeSnapshot(processingUnit);
		log.info("initialStateSnapshot = {}", initialStateSnapshot.toJsonWithoutZeros());

		ProcessingUnitInstanceStateSnapshot latestStateSnapshot = initialStateSnapshot;
		boolean lastIterationRebalanced;
		do {
			lastIterationRebalanced = doRebalance(latestStateSnapshot);

			if (lastIterationRebalanced) {
				final ProcessingUnitInstanceStateSnapshot processingUnitInstanceStateSnapshotAfter = takeSnapshot(processingUnit);
				log.info("processingUnitInstanceStateSnapshotAfter = {}", processingUnitInstanceStateSnapshotAfter.toJsonWithoutZeros());
				latestStateSnapshot = processingUnitInstanceStateSnapshotAfter;
			}
		} while (!onceOnly && lastIterationRebalanced);
	}

	private boolean doRebalance(ProcessingUnitInstanceStateSnapshot stateSnapshotBefore) {
		boolean rebalanced = rebalanceByBreakDownOnEachPartition(stateSnapshotBefore);
		if (rebalanced) {
			return true;
		}
		rebalanced = rebalanceByBreakDown(stateSnapshotBefore.processingUnitInstanceRepartitionSnapshot.actualTotalCounts, stateSnapshotBefore);
		if (rebalanced) {
			return true;
		}
		return false;
	}

	private boolean rebalanceByBreakDownOnEachPartition(ProcessingUnitInstanceStateSnapshot processingUnitInstanceStateSnapshotBefore) {

		for (Map.Entry<Integer, ProcessingUnitInstanceRepartitionSnapshot> entry : processingUnitInstanceStateSnapshotBefore.processingUnitInstanceRepartitionSnapshotPerPartition.entrySet()) {
			final Integer partitionId = entry.getKey();
			final ProcessingUnitInstanceRepartitionSnapshot snapshotForPartition = entry.getValue();
			boolean rebalanceDone = rebalanceByBreakDown(snapshotForPartition.actualTotalCounts, processingUnitInstanceStateSnapshotBefore);
			if (rebalanceDone) {
				log.info("Partition Id {} has been relocated", partitionId);
				return true;
			}
		}

		log.info("Does not need to relocate any PU Instance");
		return false;
	}

	private boolean rebalanceByBreakDown(ProcessingUnitInstanceBreakdownSnapshot breakdown, ProcessingUnitInstanceStateSnapshot processingUnitInstanceStateSnapshotBefore) {
		MinAndMax<String> minAndMaxByMachine = findMinAndMax(breakdown.countByMachine);
		boolean needsRebalancedByMachine = needsRebalanced(minAndMaxByMachine);
		if (needsRebalancedByMachine) {
			rebalanceByMachine(processingUnitInstanceStateSnapshotBefore.processingUnitInstances, minAndMaxByMachine);
			return true;
		}
		MinAndMax<String> minAndMaxByGSC = findMinAndMax(breakdown.countByGSC);
		boolean needsRebalancedByGSC = needsRebalanced(minAndMaxByGSC);
		if (needsRebalancedByGSC) {
			rebalanceByGSC(processingUnitInstanceStateSnapshotBefore.processingUnitInstances, minAndMaxByGSC);
			return true;
		}
		log.info("Does not need to relocate any PU Instance");
		return false;
	}

	public ProcessingUnitInstanceStateSnapshot takeSnapshot(ProcessingUnit processingUnit) {

		final GridServiceContainer[] allMatchingContainersForPu = puRelocateService.findBestContainersToRelocate(processingUnit, machine -> true, gsc -> true);
		final long allMatchingContainersForPuCount = allMatchingContainersForPu.length;
		log.debug("allMatchingContainersForPuCount = {}", allMatchingContainersForPuCount);

		final ProcessingUnitInstanceBreakdownSnapshot potentialCounts;
		{
			final AtomicLongMap<String> potentialCountByMachine = AtomicLongMap.create();
			for (GridServiceContainer gsc : allMatchingContainersForPu) {
				String hostName = gsc.getMachine().getHostName();
				potentialCountByMachine.addAndGet(hostName, 1);
			}
			//
			final AtomicLongMap<String> potentialCountByZone = AtomicLongMap.create();
			for (GridServiceContainer gsc : allMatchingContainersForPu) {
				ExactZonesConfig exactZones = gsc.getExactZones();
				for (String zone : exactZones.getZones()) {
					potentialCountByZone.addAndGet(zone, 1);
				}
			}
			//
			final AtomicLongMap<String> potentialCountByGSC = AtomicLongMap.create();
			for (GridServiceContainer gsc : allMatchingContainersForPu) {
				String gscId = gsc.getId();
				potentialCountByGSC.addAndGet(gscId, 1);
			}
			//
			log.debug("potentialCountByZone = {}", potentialCountByZone);
			log.debug("potentialCountByMachine = {}", potentialCountByMachine);
			log.debug("potentialCountByGSC = {}", potentialCountByGSC);

			potentialCounts = ProcessingUnitInstanceBreakdownSnapshot.builder()
					.countByZone(potentialCountByZone)
					.countByMachine(potentialCountByMachine)
					.countByGSC(potentialCountByGSC)
					.build();
		}

		final ProcessingUnitInstanceBreakdownSnapshot actualTotalCounts = potentialCounts.createNewWithZeroCounts();
		final ProcessingUnitInstanceBreakdownSnapshot actualPrimaryCounts = potentialCounts.createNewWithZeroCounts();
		final ProcessingUnitInstanceBreakdownSnapshot actualBackupCounts = potentialCounts.createNewWithZeroCounts();

		//
		final ProcessingUnitInstance[] processingUnitInstances = processingUnit.getInstances();

		final List<Integer> partitionsIds = Arrays.stream(processingUnitInstances).map(processingUnitInstance -> processingUnitInstance.getPartition().getPartitionId()).collect(Collectors.toList());

		final SortedMap<Integer, ProcessingUnitInstanceRepartitionSnapshot> processingUnitInstanceRepartitionSnapshotPerPartition = new TreeMap<>();
		for (Integer partitionId : partitionsIds) {
			processingUnitInstanceRepartitionSnapshotPerPartition.put(partitionId,
					new ProcessingUnitInstanceRepartitionSnapshot(
							potentialCounts.createNewWithZeroCounts(),
							potentialCounts.createNewWithZeroCounts(),
							potentialCounts.createNewWithZeroCounts()
					)
			);
		}

		IsPrimaryStatefulProcessingUnitPredicate isPrimaryStatefulProcessingUnitPredicate = new IsPrimaryStatefulProcessingUnitPredicate();
		IsBackupStatefulProcessingUnitPredicate isBackupStatefulProcessingUnitPredicate = new IsBackupStatefulProcessingUnitPredicate();
		//
		for (ProcessingUnitInstance puInstance : processingUnitInstances) {
			final int partitionId = puInstance.getPartition().getPartitionId();
			GridServiceContainer gridServiceContainer = puInstance.getGridServiceContainer();
			String hostName = gridServiceContainer.getMachine().getHostName();
			actualTotalCounts.countByMachine.addAndGet(hostName, 1);
			processingUnitInstanceRepartitionSnapshotPerPartition.get(partitionId).actualTotalCounts.countByMachine.addAndGet(hostName, 1);
			boolean isPrimary = isPrimaryStatefulProcessingUnitPredicate.test(puInstance);
			if (isPrimary) {
				actualPrimaryCounts.countByMachine.addAndGet(hostName, 1);
				processingUnitInstanceRepartitionSnapshotPerPartition.get(partitionId).actualPrimaryCounts.countByMachine.addAndGet(hostName, 1);
			} else {
				boolean isBackup = isBackupStatefulProcessingUnitPredicate.test(puInstance);
				if (isBackup) {
					actualBackupCounts.countByMachine.addAndGet(hostName, 1);
					processingUnitInstanceRepartitionSnapshotPerPartition.get(partitionId).actualBackupCounts.countByMachine.addAndGet(hostName, 1);
				}
			}
		}
		//
		for (ProcessingUnitInstance puInstance : processingUnitInstances) {
			final int partitionId = puInstance.getPartition().getPartitionId();
			GridServiceContainer gridServiceContainer = puInstance.getGridServiceContainer();
			ExactZonesConfig exactZones = gridServiceContainer.getExactZones();
			final boolean isPrimary = isPrimaryStatefulProcessingUnitPredicate.test(puInstance);
			final boolean isBackup = isBackupStatefulProcessingUnitPredicate.test(puInstance);
			for (String zone : exactZones.getZones()) {
				actualTotalCounts.countByZone.addAndGet(zone, 1);
				processingUnitInstanceRepartitionSnapshotPerPartition.get(partitionId).actualTotalCounts.countByZone.addAndGet(zone, 1);
				if (isPrimary) {
					actualPrimaryCounts.countByZone.addAndGet(zone, 1);
					processingUnitInstanceRepartitionSnapshotPerPartition.get(partitionId).actualPrimaryCounts.countByZone.addAndGet(zone, 1);
				} else {
					if (isBackup) {
						actualBackupCounts.countByZone.addAndGet(zone, 1);
						processingUnitInstanceRepartitionSnapshotPerPartition.get(partitionId).actualBackupCounts.countByZone.addAndGet(zone, 1);
					}
				}
			}
		}
		//
		for (ProcessingUnitInstance puInstance : processingUnitInstances) {
			final int partitionId = puInstance.getPartition().getPartitionId();
			GridServiceContainer gridServiceContainer = puInstance.getGridServiceContainer();
			String gscId = gridServiceContainer.getId();
			actualTotalCounts.countByGSC.addAndGet(gscId, 1);
			processingUnitInstanceRepartitionSnapshotPerPartition.get(partitionId).actualTotalCounts.countByGSC.addAndGet(gscId, 1);
			final boolean isPrimary = isPrimaryStatefulProcessingUnitPredicate.test(puInstance);
			if (isPrimary) {
				actualPrimaryCounts.countByGSC.addAndGet(gscId, 1);
				processingUnitInstanceRepartitionSnapshotPerPartition.get(partitionId).actualPrimaryCounts.countByGSC.addAndGet(gscId, 1);
			} else {
				boolean isBackup = isBackupStatefulProcessingUnitPredicate.test(puInstance);
				if (isBackup) {
					actualBackupCounts.countByGSC.addAndGet(gscId, 1);
					processingUnitInstanceRepartitionSnapshotPerPartition.get(partitionId).actualBackupCounts.countByGSC.addAndGet(gscId, 1);
				}
			}
		}
		//
		log.debug("actualTotalCounts = {}", actualTotalCounts);
		log.debug("actualPrimaryCounts = {}", actualPrimaryCounts);
		log.debug("actualBackupCounts = {}", actualBackupCounts);
		log.debug("processingUnitInstanceRepartitionSnapshotPerPartition = {}", processingUnitInstanceRepartitionSnapshotPerPartition);

		return new ProcessingUnitInstanceStateSnapshot(
				processingUnitInstances,
				potentialCounts,
				new ProcessingUnitInstanceRepartitionSnapshot(
						actualTotalCounts,
						actualPrimaryCounts,
						actualBackupCounts
				),
				processingUnitInstanceRepartitionSnapshotPerPartition
		);
	}

	private void doRelocate(ProcessingUnitInstance processingUnitInstanceToRelocate, MinAndMax<String> minAndMax, Predicate<Machine> targetMachinePredicate) {
		ProcessingUnitPartition processingUnitPartition = processingUnitInstanceToRelocate.getPartition();
		final int partitionId = processingUnitPartition.getPartitionId() + 1;
		final int primaryPartitionId = processingUnitPartition.getPrimary().getPartition().getPartitionId() + 1;
		//final String primaryOrBackupIndicator = (partitionId == primaryPartitionId) ? "P" : "B";
		final String primaryOrBackupIndicator = (processingUnitInstanceToRelocate.getSpaceInstance() != null && processingUnitInstanceToRelocate.getSpaceInstance().getMode() == SpaceMode.PRIMARY) ? "P" : "B";

		log.warn("Will relocate instance of Processing Unit Instance {} (Partition #{} ({})) from GSC {} to GSC {}", processingUnitInstanceToRelocate.getId(), partitionId, primaryOrBackupIndicator, minAndMax.getMax().getKey(), minAndMax.getMin().getKey());
		userConfirmationService.askConfirmationAndWait();

		//
		puRelocateService.relocatePuInstance(processingUnitInstanceToRelocate, targetMachinePredicate, true);
	}

	private static Set<String> extractIds(Collection<ProcessingUnitInstance> processingUnitInstances) {
		return new LinkedHashSet<>(processingUnitInstances.stream().map(ProcessingUnitInstance::getId).collect(Collectors.toSet()));
	}

	private void rebalanceByMachine(ProcessingUnitInstance[] processingUnitInstances, MinAndMax<String> minAndMaxByMachine) {
		log.info("Rebalancing ProcessingUnit by Machine : minAndMaxByMachine = {}", minAndMaxByMachine);

		Set<Integer> partitionIdsOnMachineWithMinCount = Arrays.stream(processingUnitInstances)
				.filter(processingUnitInstance -> processingUnitInstance.getGridServiceContainer().getMachine().getHostName().equals(minAndMaxByMachine.getMin().getKey()))
				.map(processingUnitInstance -> processingUnitInstance.getPartition().getPartitionId())
				.collect(Collectors.toSet());

		log.info("partitionIdsOnMachineWithMinCount = {}", partitionIdsOnMachineWithMinCount);

		List<ProcessingUnitInstance> candidateProcessingUnitInstancesToRelocate = Arrays.stream(processingUnitInstances)
				.filter(processingUnitInstance -> processingUnitInstance.getGridServiceContainer().getMachine().getHostName().equals(minAndMaxByMachine.getMax().getKey()))
				.filter(processingUnitInstance -> !partitionIdsOnMachineWithMinCount.contains(processingUnitInstance.getPartition().getPartitionId()))
				.sorted(new BackupFirstProcessingUnitInstanceComparator())
				.collect(Collectors.toList());

		log.info("candidateProcessingUnitInstancesToRelocate = {}", extractIds(candidateProcessingUnitInstancesToRelocate));

		final ProcessingUnitInstance processingUnitInstanceToRelocate = candidateProcessingUnitInstancesToRelocate.get(0);
		final Predicate<Machine> targetMachinePredicate = new MachineWithSameNamePredicate(minAndMaxByMachine.getMin().getKey());

		doRelocate(processingUnitInstanceToRelocate, minAndMaxByMachine, targetMachinePredicate);
	}

	private void rebalanceByGSC(ProcessingUnitInstance[] processingUnitInstances, MinAndMax<String> minAndMaxByGSC) {
		log.info("Rebalancing ProcessingUnit by GSC ...");

		Set<Integer> partitionIdsOnMachineWithMinCount = Arrays.stream(processingUnitInstances)
				.filter(processingUnitInstance -> processingUnitInstance.getGridServiceContainer().getId().equals(minAndMaxByGSC.getMin().getKey()))
				.map(processingUnitInstance -> processingUnitInstance.getPartition().getPartitionId())
				.collect(Collectors.toSet());

		log.info("partitionIdsOnMachineWithMinCount = {}", partitionIdsOnMachineWithMinCount);

		List<ProcessingUnitInstance> candidateProcessingUnitInstancesToRelocate = Arrays.stream(processingUnitInstances)
				.filter(processingUnitInstance -> processingUnitInstance.getGridServiceContainer().getId().equals(minAndMaxByGSC.getMax().getKey()))
				.filter(processingUnitInstance -> !partitionIdsOnMachineWithMinCount.contains(processingUnitInstance.getPartition().getPartitionId()))
				.sorted(new BackupFirstProcessingUnitInstanceComparator())
				.collect(Collectors.toList());

		log.info("candidateProcessingUnitInstancesToRelocate = {}", extractIds(candidateProcessingUnitInstancesToRelocate));

		final ProcessingUnitInstance processingUnitInstanceToRelocate = candidateProcessingUnitInstancesToRelocate.get(0);
		final Predicate<Machine> targetMachinePredicate = machine -> true;

		doRelocate(processingUnitInstanceToRelocate, minAndMaxByGSC, targetMachinePredicate);
	}

	private boolean needsRebalanced(MinAndMax<String> minAndMax) {
		return minAndMax != null && minAndMax.getMax().getValue() > minAndMax.getMin().getValue() + 1;
	}

}

