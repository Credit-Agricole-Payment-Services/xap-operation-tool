package gca.in.xap.tools.operationtool.service;

import lombok.NonNull;
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

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Predicate;

@Component
@Slf4j
public class DefaultPuRelocateService implements PuRelocateService {

	@Autowired
	@Lazy
	private XapService xapService;

	@Autowired
	private IdExtractor idExtractor;

	@Autowired
	private DemoteService demoteService;

	private Predicate<GridServiceContainer> createContainerPredicate(@NonNull RequiredZonesConfig puRequiredContainerZones) {
		return gsc -> {
			final ExactZonesConfig containerExactZones = gsc.getExactZones();
			return puRequiredContainerZones.isSatisfiedBy(containerExactZones);
		};
	}

	@Override
	public void relocatePuInstance(
			@NonNull ProcessingUnitInstance puInstance,
			@NonNull Predicate<Machine> targetMachinePredicate,
			boolean await,
			boolean demoteFirst,
			Duration demoteMaxSuspendDuration
	) {
		final GridServiceContainer sourceContainer = puInstance.getGridServiceContainer();
		final GridServiceContainer gscWherePuIsCurrentlyRunning = puInstance.getGridServiceContainer();
		final Predicate<GridServiceContainer> gridServiceContainerPredicate = gsc -> !gsc.getId().equals(gscWherePuIsCurrentlyRunning.getId());
		final ProcessingUnit processingUnit = puInstance.getProcessingUnit();
		//
		final GridServiceContainer destinationContainer = findBestContainerToRelocate(processingUnit, targetMachinePredicate, gridServiceContainerPredicate);

		demoteService.demotePrimarySpaceInstances(puInstance, demoteMaxSuspendDuration);

		log.info("PU instance '{}' of PU '{}' will be relocated from {} ({}) to {} ({}) ... (if this is allowed by the SLA)",
				idExtractor.extractProcessingUnitInstanceNameAndDescription(puInstance),
				processingUnit.getName(),
				sourceContainer.getId(),
				sourceContainer.getExactZones(),
				destinationContainer.getId(),
				destinationContainer.getExactZones()
		);
		if (await) {
			puInstance.relocateAndWait(destinationContainer);
		} else {
			puInstance.relocate(destinationContainer);
		}
	}

	@Override
	public GridServiceContainer[] findBestContainersToRelocate(
			@NonNull ProcessingUnit processingUnit,
			@NonNull Predicate<Machine> targetMachinePredicate,
			@NonNull Predicate<GridServiceContainer> gridServiceContainerPredicate
	) {
		//
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
				.filter(gsc -> targetMachinePredicate.test(gsc.getMachine()))
				.filter(gridServiceContainerPredicate)
				.filter(createContainerPredicate(puRequiredContainerZones))
				.sorted(comparator).toArray(GridServiceContainer[]::new);
	}

	@Override
	public GridServiceContainer findBestContainerToRelocate(
			@NonNull ProcessingUnit processingUnit,
			@NonNull Predicate<Machine> targetMachinePredicate,
			@NonNull Predicate<GridServiceContainer> gridServiceContainerPredicate
	) {
		final RequiredZonesConfig puRequiredContainerZones = processingUnit.getRequiredContainerZones();
		return Arrays.stream(findBestContainersToRelocate(processingUnit, targetMachinePredicate, gridServiceContainerPredicate))
				.findFirst()
				.orElseThrow(() -> new UnsupportedOperationException("Did not find any GSC matching requirements, with puRequiredContainerZones = " + puRequiredContainerZones));
	}

}
