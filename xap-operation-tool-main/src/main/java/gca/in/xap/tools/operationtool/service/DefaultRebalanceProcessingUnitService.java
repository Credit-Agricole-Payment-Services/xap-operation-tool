package gca.in.xap.tools.operationtool.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.util.concurrent.AtomicLongMap;
import gca.in.xap.tools.operationtool.comparators.processingunitinstance.BackupFirstProcessingUnitInstanceComparator;
import gca.in.xap.tools.operationtool.predicates.machine.MachineWithSameNamePredicate;
import gca.in.xap.tools.operationtool.predicates.pu.IsBackupStatefulProcessingUnitPredicate;
import gca.in.xap.tools.operationtool.predicates.pu.IsPrimaryStatefulProcessingUnitPredicate;
import gca.in.xap.tools.operationtool.userinput.UserConfirmationService;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.zone.config.ExactZonesConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
	public void rebalanceProcessingUnit(String processingUnitName, RestartStrategy restartStrategy) {
		log.info("processingUnitName = {}", processingUnitName);
		ProcessingUnit processingUnit = xapService.findProcessingUnitByName(processingUnitName);
		log.info("processingUnit = {}", processingUnit);
		if (processingUnit == null) {
			throw new IllegalStateException("ProcessingUnit with name " + processingUnitName + " was not found");
		}

		final ProcessingUnitInstanceStateSnapshot processingUnitInstanceStateSnapshotBefore = takeSnapshot(processingUnit);
		log.info("processingUnitInstanceStateSnapshotBefore = {}", processingUnitInstanceStateSnapshotBefore.toJson());

		rebalanceByBreakDown(processingUnitInstanceStateSnapshotBefore.processingUnitInstanceRepartitionSnapshot.actualTotalCounts, processingUnitInstanceStateSnapshotBefore);

		final ProcessingUnitInstanceStateSnapshot processingUnitInstanceStateSnapshotAfter = takeSnapshot(processingUnit);
		log.info("processingUnitInstanceStateSnapshotAfter = {}", processingUnitInstanceStateSnapshotAfter.toJson());
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

	@Data
	public static class ProcessingUnitInstanceStateSnapshot {

		private static final ObjectMapper objectMapper = new ObjectMapperFactory().createObjectMapper();

		static {
			objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

			objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

			SimpleModule module = new SimpleModule();
			module.addSerializer(AtomicLongMap.class, new AtomicLongMapSerializer());
			objectMapper.registerModule(module);
		}

		@JsonIgnore
		private final ProcessingUnitInstance[] processingUnitInstances;

		private final ProcessingUnitInstanceBreakdownSnapshot potentialCounts;

		private final ProcessingUnitInstanceRepartitionSnapshot processingUnitInstanceRepartitionSnapshot;

		/**
		 * key : partition ID
		 * value : snapshot for that partition
		 */
		private final SortedMap<Integer, ProcessingUnitInstanceRepartitionSnapshot> processingUnitInstanceRepartitionSnapshotPerPartition;

		public String toJson() {
			try {
				return objectMapper.writeValueAsString(this);
			} catch (JsonProcessingException e) {
				throw new RuntimeException(e);
			}
		}

	}

	@Data
	public static class ProcessingUnitInstanceRepartitionSnapshot {

		private final ProcessingUnitInstanceBreakdownSnapshot actualTotalCounts;

		private final ProcessingUnitInstanceBreakdownSnapshot actualPrimaryCounts;

		private final ProcessingUnitInstanceBreakdownSnapshot actualBackupCounts;

	}

	@Data
	public static class ProcessingUnitInstanceBreakdownSnapshot {

		private final AtomicLongMap<String> countByMachine;
		private final AtomicLongMap<String> countByZone;
		private final AtomicLongMap<String> countByGSC;

		public ProcessingUnitInstanceBreakdownSnapshot createNewWithZeroCounts() {
			return new ProcessingUnitInstanceBreakdownSnapshot(
					initAtomicLongMapCounterWithZeroValues(countByMachine),
					initAtomicLongMapCounterWithZeroValues(countByZone),
					initAtomicLongMapCounterWithZeroValues(countByGSC)
			);
		}

		private static AtomicLongMap<String> initAtomicLongMapCounterWithZeroValues(AtomicLongMap<String> potentialCounter) {
			final AtomicLongMap<String> newInstance = AtomicLongMap.create();
			for (String key : potentialCounter.asMap().keySet()) {
				newInstance.addAndGet(key, 0);
			}
			return newInstance;
		}

	}

	public ProcessingUnitInstanceStateSnapshot takeSnapshot(ProcessingUnit processingUnit) {

		final GridServiceContainer[] allMatchingContainersForPu = puRelocateService.findBestContainersToRelocate(processingUnit, machine -> true, gsc -> true);
		final long allMatchingContainersForPuCount = allMatchingContainersForPu.length;
		log.info("allMatchingContainersForPuCount = {}", allMatchingContainersForPuCount);

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
			log.info("potentialCountByZone = {}", potentialCountByZone);
			log.info("potentialCountByMachine = {}", potentialCountByMachine);
			log.info("potentialCountByGSC = {}", potentialCountByGSC);

			potentialCounts = new ProcessingUnitInstanceBreakdownSnapshot(potentialCountByMachine, potentialCountByZone, potentialCountByGSC);
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
		log.info("actualTotalCounts = {}", actualTotalCounts);
		log.info("actualPrimaryCounts = {}", actualPrimaryCounts);
		log.info("actualBackupCounts = {}", actualBackupCounts);
		log.info("processingUnitInstanceRepartitionSnapshotPerPartition = {}", processingUnitInstanceRepartitionSnapshotPerPartition);

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

	private void rebalanceByMachine(ProcessingUnitInstance[] processingUnitInstances, MinAndMax<String> minAndMaxByMachine) {
		log.info("Rebalancing ProcessingUnit by Machine : minAndMaxByMachine = {}", minAndMaxByMachine);
		Optional<ProcessingUnitInstance> processingUnitInstanceToRelocate = Arrays.stream(processingUnitInstances)
				.filter(processingUnitInstance -> processingUnitInstance.getGridServiceContainer().getMachine().getHostName().equals(minAndMaxByMachine.getMax().getKey()))
				.sorted(new BackupFirstProcessingUnitInstanceComparator())
				.findFirst();
		ProcessingUnitInstance processingUnitInstance = processingUnitInstanceToRelocate.get();

		log.warn("Will relocate instance of Processing Unit Instance {} from Machine {} to Machine {}", processingUnitInstance.getId(), minAndMaxByMachine.getMax().getKey(), minAndMaxByMachine.getMin().getKey());
		userConfirmationService.askConfirmationAndWait();

		final Predicate<Machine> targetMachinePredicate = new MachineWithSameNamePredicate(minAndMaxByMachine.getMin().getKey());
		//
		puRelocateService.relocatePuInstance(processingUnitInstance, targetMachinePredicate, true);
	}

	private void rebalanceByGSC(ProcessingUnitInstance[] processingUnitInstances, MinAndMax<String> minAndMaxByGSC) {
		log.info("Rebalancing ProcessingUnit by GSC ...");
		Optional<ProcessingUnitInstance> processingUnitInstanceToRelocate = Arrays.stream(processingUnitInstances)
				.filter(processingUnitInstance -> processingUnitInstance.getGridServiceContainer().getId().equals(minAndMaxByGSC.getMax().getKey()))
				.sorted(new BackupFirstProcessingUnitInstanceComparator())
				.findFirst();
		ProcessingUnitInstance processingUnitInstance = processingUnitInstanceToRelocate.get();

		log.warn("Will relocate instance of Processing Unit Instance {} from GSC {} to GSC {}", processingUnitInstance.getId(), minAndMaxByGSC.getMax().getKey(), minAndMaxByGSC.getMin().getKey());
		userConfirmationService.askConfirmationAndWait();

		final Predicate<Machine> targetMachinePredicate = machine -> true;
		//
		puRelocateService.relocatePuInstance(processingUnitInstance, targetMachinePredicate, true);
	}

	private boolean needsRebalanced(MinAndMax<String> minAndMax) {
		return minAndMax != null && minAndMax.getMax().getValue() > minAndMax.getMin().getValue() + 1;
	}


	@Data
	public static class MinAndMax<T> {
		private final Map.Entry<T, Long> min;
		private final Map.Entry<T, Long> max;
	}

	public static <T> MinAndMax<T> findMinAndMax(AtomicLongMap<T> atomicLongMap) {
		if (atomicLongMap.isEmpty()) {
			return null;
		}
		Map.Entry<T, Long> min = null;
		Map.Entry<T, Long> max = null;
		for (Map.Entry<T, Long> entry : atomicLongMap.asMap().entrySet()) {
			if (min == null) {
				min = entry;
			} else {
				if (entry.getValue() < min.getValue()) {
					min = entry;
				}
			}
			if (max == null) {
				max = entry;
			} else {
				if (entry.getValue() > max.getValue()) {
					max = entry;
				}
			}
		}
		return new MinAndMax<>(min, max);
	}

	public static class AtomicLongMapSerializer extends StdSerializer<AtomicLongMap> {

		public AtomicLongMapSerializer() {
			this(null);
		}

		public AtomicLongMapSerializer(Class<AtomicLongMap> t) {
			super(t);
		}

		@Override
		public void serialize(AtomicLongMap atomicLongMap, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
			doSerialize(atomicLongMap, jsonGenerator, serializerProvider);
		}

		public static <T> void doSerialize(AtomicLongMap<T> atomicLongMap, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
			jsonGenerator.writeStartObject();
			SortedMap<T, Long> sortedMap = new TreeMap<>(atomicLongMap.asMap());
			for (Map.Entry<T, Long> entry : sortedMap.entrySet()) {

				String key = String.valueOf(entry.getKey());
				long value = entry.getValue();
				jsonGenerator.writeNumberField(key, value);
			}
			jsonGenerator.writeEndObject();
		}

	}

}

