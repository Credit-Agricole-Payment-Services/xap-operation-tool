package gca.in.xap.tools.operationtool.service;

import com.google.common.util.concurrent.AtomicLongMap;
import gca.in.xap.tools.operationtool.predicates.machine.MachineWithSameNamePredicate;
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

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

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

		final ProcessingUnitInstanceRepartitionSnapshot processingUnitInstanceRepartitionSnapshotBefore = takeSnapshot(processingUnit);
		log.info("processingUnitInstanceRepartitionSnapshotBefore = {}", processingUnitInstanceRepartitionSnapshotBefore);

		MinAndMax<String> minAndMaxByMachine = findMinAndMax(processingUnitInstanceRepartitionSnapshotBefore.actualCountByMachine);
		boolean needsRebalancedByMachine = needsRebalanced(minAndMaxByMachine);
		if (needsRebalancedByMachine) {
			rebalanceByMachine(processingUnitInstanceRepartitionSnapshotBefore.processingUnitInstances, minAndMaxByMachine);
		} else {
			MinAndMax<String> minAndMaxByGSC = findMinAndMax(processingUnitInstanceRepartitionSnapshotBefore.actualCountByGSC);
			boolean needsRebalancedByGSC = needsRebalanced(minAndMaxByGSC);
			if (needsRebalancedByGSC) {
				rebalanceByGSC(processingUnitInstanceRepartitionSnapshotBefore.processingUnitInstances, minAndMaxByGSC);
			} else {
				log.info("Does not need to relocate any PU Instance");
				return;
			}
		}

		final ProcessingUnitInstanceRepartitionSnapshot processingUnitInstanceRepartitionSnapshotAfter = takeSnapshot(processingUnit);
		log.info("processingUnitInstanceRepartitionSnapshotAfter = {}", processingUnitInstanceRepartitionSnapshotAfter);
	}

	@Data
	public static class ProcessingUnitInstanceRepartitionSnapshot {
		private final ProcessingUnitInstance[] processingUnitInstances;

		private final AtomicLongMap<String> potentialCountByMachine;
		private final AtomicLongMap<String> potentialCountByZone;
		private final AtomicLongMap<String> potentialCountByGSC;

		private final AtomicLongMap<String> actualCountByMachine;
		private final AtomicLongMap<String> actualCountByZone;
		private final AtomicLongMap<String> actualCountByGSC;
	}

	public ProcessingUnitInstanceRepartitionSnapshot takeSnapshot(ProcessingUnit processingUnit) {

		final GridServiceContainer[] allMatchingContainersForPu = puRelocateService.findBestContainersToRelocate(processingUnit, machine -> true, gsc -> true);
		final long allMatchingContainersForPuCount = allMatchingContainersForPu.length;
		log.info("allMatchingContainersForPuCount = {}", allMatchingContainersForPuCount);

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

		//
		ProcessingUnitInstance[] processingUnitInstances = processingUnit.getInstances();
		//
		final AtomicLongMap<String> actualCountByMachine = initAtomicLongMapCounterWithZeroValues(potentialCountByMachine);
		for (ProcessingUnitInstance puInstance : processingUnitInstances) {
			GridServiceContainer gridServiceContainer = puInstance.getGridServiceContainer();
			String hostName = gridServiceContainer.getMachine().getHostName();
			actualCountByMachine.addAndGet(hostName, 1);
		}
		//
		final AtomicLongMap<String> actualCountByZone = initAtomicLongMapCounterWithZeroValues(potentialCountByZone);
		for (ProcessingUnitInstance puInstance : processingUnitInstances) {
			GridServiceContainer gridServiceContainer = puInstance.getGridServiceContainer();
			ExactZonesConfig exactZones = gridServiceContainer.getExactZones();
			for (String zone : exactZones.getZones()) {
				actualCountByZone.addAndGet(zone, 1);
			}
		}
		//
		final AtomicLongMap<String> actualCountByGSC = initAtomicLongMapCounterWithZeroValues(potentialCountByGSC);
		for (ProcessingUnitInstance puInstance : processingUnitInstances) {
			GridServiceContainer gridServiceContainer = puInstance.getGridServiceContainer();
			String gscId = gridServiceContainer.getId();
			actualCountByGSC.addAndGet(gscId, 1);
		}
		//
		log.info("actualCountByZone = {}", actualCountByZone);
		log.info("actualCountByMachine = {}", actualCountByMachine);
		log.info("actualCountByGSC = {}", actualCountByGSC);

		return new ProcessingUnitInstanceRepartitionSnapshot(
				processingUnitInstances,
				potentialCountByMachine, potentialCountByZone, potentialCountByGSC,
				actualCountByMachine, actualCountByZone, actualCountByGSC
		);
	}

	private void rebalanceByMachine(ProcessingUnitInstance[] processingUnitInstances, MinAndMax<String> minAndMaxByMachine) {
		Optional<ProcessingUnitInstance> processingUnitInstanceToRelocate = Arrays.stream(processingUnitInstances)
				.filter(processingUnitInstance -> processingUnitInstance.getGridServiceContainer().getMachine().getHostName().equals(minAndMaxByMachine.getMax().getKey()))
				.findFirst();
		ProcessingUnitInstance processingUnitInstance = processingUnitInstanceToRelocate.get();

		log.warn("Will relocate instance of Processing Unit Instance {} from Machine {} to Machine {}", processingUnitInstance.getId(), minAndMaxByMachine.getMax().getKey(), minAndMaxByMachine.getMin().getKey());
		userConfirmationService.askConfirmationAndWait();

		final Predicate<Machine> targetMachinePredicate = new MachineWithSameNamePredicate(minAndMaxByMachine.getMin().getKey());
		//
		puRelocateService.relocatePuInstance(processingUnitInstance, targetMachinePredicate, true);
	}

	private void rebalanceByGSC(ProcessingUnitInstance[] processingUnitInstances, MinAndMax<String> minAndMaxByGSC) {
		Optional<ProcessingUnitInstance> processingUnitInstanceToRelocate = Arrays.stream(processingUnitInstances)
				.filter(processingUnitInstance -> processingUnitInstance.getGridServiceContainer().getId().equals(minAndMaxByGSC.getMax().getKey()))
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

	private static AtomicLongMap<String> initAtomicLongMapCounterWithZeroValues(AtomicLongMap<String> potentialCounter) {
		final AtomicLongMap<String> newInstance = AtomicLongMap.create();
		for (String key : potentialCounter.asMap().keySet()) {
			newInstance.addAndGet(key, 0);
		}
		return newInstance;
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


}

