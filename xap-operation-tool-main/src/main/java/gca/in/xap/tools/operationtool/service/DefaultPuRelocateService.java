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

@Component
@Slf4j
public class DefaultPuRelocateService implements PuRelocateService {

	@Autowired
	@Lazy
	private XapService xapService;

	public void relocatePuInstance(ProcessingUnitInstance puInstance, Predicate<Machine> machinePredicate) {
		final GridServiceContainer gscWherePuIsCurrentlyRunning = puInstance.getGridServiceContainer();
		//
		final ProcessingUnit processingUnit = puInstance.getProcessingUnit();
		final RequiredZonesConfig puRequiredContainerZones = processingUnit.getRequiredContainerZones();
		log.info("Looking for a GSC with Zones configuration that matches : {}", puRequiredContainerZones);

		//
		Predicate<GridServiceContainer> containerPredicate = gsc -> {
			final ExactZonesConfig containerExactZones = gsc.getExactZones();
			return puRequiredContainerZones.isSatisfiedBy(containerExactZones);
		};

		GridServiceContainer[] containers = xapService.findContainers();

		GridServiceContainer container = Arrays.stream(containers)
				.filter(gsc -> machinePredicate.test(gsc.getMachine()))
				.filter(gsc -> !gsc.getId().equals(gscWherePuIsCurrentlyRunning.getId()))
				.filter(containerPredicate)
				.min(Comparator.comparingInt(gsc -> gsc.getProcessingUnitInstances().length))
				.orElseThrow(() -> new UnsupportedOperationException("Did not find any GSC matching requirements, with puRequiredContainerZones = " + puRequiredContainerZones));

		log.info("Identified a matching GSC to relocate the PU instance {} of PU {} : {} (having zone config : {})",
				puInstance.getId(),
				processingUnit.getName(),
				container.getId(),
				container.getExactZones());
		puInstance.relocate(container);
	}

}
