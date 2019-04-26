package gca.in.xap.tools.operationtool.service;

import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.zone.config.ExactZonesConfig;
import org.openspaces.admin.zone.config.RequiredZonesConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Component
@Slf4j
public class DefaultPuRelocateService implements PuRelocateService {

	@Autowired
	@Lazy
	private XapService xapService;

	private Predicate<GridServiceContainer> createContainerPredicate(RequiredZonesConfig puRequiredContainerZones) {
		return gsc -> {
			final ExactZonesConfig containerExactZones = gsc.getExactZones();
			return puRequiredContainerZones.isSatisfiedBy(containerExactZones);
		};
	}

	@Override
	public void relocatePuInstance(ProcessingUnitInstance puInstance, Predicate<Machine> machinePredicate, boolean await) {
		final GridServiceContainer sourceContainer = puInstance.getGridServiceContainer();
		final GridServiceContainer destinationContainer = findBestContainerToRelocate(puInstance, machinePredicate);

		final ProcessingUnit processingUnit = puInstance.getProcessingUnit();

		log.info("PU instance {} of PU {} (having zone config : {}) that is currently running on {} will be relocated to {} (if this is allowed by the SLA)",
				puInstance.getId(),
				processingUnit.getName(),
				destinationContainer.getExactZones(),
				sourceContainer.getId(),
				destinationContainer.getId()
		);

		if (await) {
			puInstance.relocateAndWait(destinationContainer);
		} else {
			puInstance.relocate(destinationContainer);
		}
	}

	@Override
	public Stream<GridServiceContainer> findBestContainersToRelocate(ProcessingUnitInstance puInstance, Predicate<Machine> machinePredicate) {
		final GridServiceContainer gscWherePuIsCurrentlyRunning = puInstance.getGridServiceContainer();
		//
		final ProcessingUnit processingUnit = puInstance.getProcessingUnit();
		final RequiredZonesConfig puRequiredContainerZones = processingUnit.getRequiredContainerZones();
		log.info("Looking for a GSC with Zones configuration that matches : {}", puRequiredContainerZones);

		final GridServiceContainer[] containers = xapService.findContainers();

		// we want to use the GSC
		// that has the less ProcessingUnitInstances
		// and that is running on the machine that as the less ProcessingUnitInstances
		final Comparator<GridServiceContainer> comparator = Comparator
				.comparingInt((GridServiceContainer gsc) -> gsc.getProcessingUnitInstances().length)
				.thenComparingInt(gsc -> gsc.getMachine().getProcessingUnitInstances().length);

		return Arrays.stream(containers)
				.filter(gsc -> machinePredicate.test(gsc.getMachine()))
				.filter(gsc -> !gsc.getId().equals(gscWherePuIsCurrentlyRunning.getId()))
				.filter(createContainerPredicate(puRequiredContainerZones))
				.sorted(comparator);
	}

	@Override
	public GridServiceContainer findBestContainerToRelocate(ProcessingUnitInstance puInstance, Predicate<Machine> machinePredicate) {
		final ProcessingUnit processingUnit = puInstance.getProcessingUnit();
		final RequiredZonesConfig puRequiredContainerZones = processingUnit.getRequiredContainerZones();
		return findBestContainersToRelocate(puInstance, machinePredicate)
				.findFirst()
				.orElseThrow(() -> new UnsupportedOperationException("Did not find any GSC matching requirements, with puRequiredContainerZones = " + puRequiredContainerZones));
	}

}
