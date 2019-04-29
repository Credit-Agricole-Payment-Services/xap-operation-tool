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

		MinAndMax<String> minAndMax = findMinAndMax(actualCountByMachine);
		if (minAndMax != null && minAndMax.getMax().getValue() > minAndMax.getMin().getValue() + 1) {
			Optional<ProcessingUnitInstance> processingUnitInstanceToRelocate = Arrays.stream(processingUnitInstances).filter(processingUnitInstance -> processingUnitInstance.getGridServiceContainer().getMachine().getHostName().equals(minAndMax.getMax().getKey())).findFirst();
			ProcessingUnitInstance processingUnitInstance = processingUnitInstanceToRelocate.get();

			log.info("Will relocate instance of Processing Unit Instance {} from Machine {} to Machine {}", processingUnitInstance.getId(), minAndMax.getMax().getKey(), minAndMax.getMin().getKey());
			userConfirmationService.askConfirmationAndWait();

			final Predicate<Machine> targetMachinePredicate = new MachineWithSameNamePredicate(minAndMax.getMin().getKey());
			//
			puRelocateService.relocatePuInstance(processingUnitInstance, targetMachinePredicate, true);
		} else {
			log.info("Does not need to relocate any PU Instance");
		}
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

