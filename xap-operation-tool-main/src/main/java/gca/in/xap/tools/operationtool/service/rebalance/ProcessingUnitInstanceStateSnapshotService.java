package gca.in.xap.tools.operationtool.service.rebalance;

import com.google.common.util.concurrent.AtomicLongMap;
import gca.in.xap.tools.operationtool.predicates.pu.IsBackupStatefulProcessingUnitPredicate;
import gca.in.xap.tools.operationtool.predicates.pu.IsPrimaryStatefulProcessingUnitPredicate;
import gca.in.xap.tools.operationtool.service.PuRelocateService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.zone.config.ExactZonesConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ProcessingUnitInstanceStateSnapshotService {

	@Autowired
	@Setter
	private PuRelocateService puRelocateService;

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
		final ProcessingUnitInstance[] allProcessingUnitInstances = processingUnit.getInstances();

		final List<Integer> partitionsIds = Arrays.stream(allProcessingUnitInstances).map(processingUnitInstance -> processingUnitInstance.getPartition().getPartitionId()).collect(Collectors.toList());

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
		for (ProcessingUnitInstance puInstance : allProcessingUnitInstances) {
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
		for (ProcessingUnitInstance puInstance : allProcessingUnitInstances) {
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
		for (ProcessingUnitInstance puInstance : allProcessingUnitInstances) {
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
				allProcessingUnitInstances,
				potentialCounts,
				new ProcessingUnitInstanceRepartitionSnapshot(
						actualTotalCounts,
						actualPrimaryCounts,
						actualBackupCounts
				),
				processingUnitInstanceRepartitionSnapshotPerPartition
		);
	}

}
