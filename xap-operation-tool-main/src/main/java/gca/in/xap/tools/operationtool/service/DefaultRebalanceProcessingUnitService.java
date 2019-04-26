package gca.in.xap.tools.operationtool.service;

import com.google.common.util.concurrent.AtomicLongMap;
import gca.in.xap.tools.operationtool.userinput.UserConfirmationService;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.pu.ProcessingUnit;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.zone.config.ExactZonesConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultRebalanceProcessingUnitService implements RebalanceProcessingUnitService {

	@Autowired
	private PuRelocateService puRelocateService;

	@Autowired
	@Lazy
	private XapService xapService;

	@Autowired
	private UserConfirmationService userConfirmationService;

	@Override
	public void rebalanceProcessingUnit(String processingUnitName, RestartStrategy restartStrategy) {
		log.info("processingUnitName = {}", processingUnitName);
		ProcessingUnit processingUnit = xapService.findProcessingUnitByName(processingUnitName);
		log.info("processingUnit = {}", processingUnit);
		if (processingUnit == null) {
			throw new IllegalStateException("ProcessingUnit with name " + processingUnitName + " was not found");
		}
		//
		ProcessingUnitInstance[] processingUnitInstances = processingUnit.getInstances();
		//
		final AtomicLongMap<String> countByMachine = AtomicLongMap.create();
		for (ProcessingUnitInstance puInstance : processingUnitInstances) {
			String hostName = puInstance.getMachine().getHostName();
			countByMachine.addAndGet(hostName, 1);
		}
		//
		final AtomicLongMap<String> countByZone = AtomicLongMap.create();
		for (ProcessingUnitInstance puInstance : processingUnitInstances) {
			ExactZonesConfig exactZones = puInstance.getGridServiceContainer().getExactZones();
			for (String zone : exactZones.getZones()) {
				countByZone.addAndGet(zone, 1);
			}
		}
		//
		final AtomicLongMap<String> countByGSC = AtomicLongMap.create();
		for (ProcessingUnitInstance puInstance : processingUnitInstances) {
			String gscId = puInstance.getGridServiceContainer().getId();
			countByGSC.addAndGet(gscId, 1);
		}
		//
		log.info("countByZone = {}", countByZone);
		log.info("countByMachine = {}", countByMachine);
		log.info("countByGSC = {}", countByGSC);
		//
		log.info("Will relocate instances of Processing Unit {} ", processingUnit.getName());
		userConfirmationService.askConfirmationAndWait();

		boolean firstIteration = true;
		for (ProcessingUnitInstance puInstance : processingUnitInstances) {
			if (!firstIteration) {
				restartStrategy.waitBetweenComponent();
			}
			puRelocateService.relocatePuInstance(puInstance, machine -> true, true);
			//
			firstIteration = false;
		}
	}

}
