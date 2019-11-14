package gca.in.xap.tools.operationtool.service;

import gca.in.xap.tools.operationtool.predicates.NotPredicate;
import gca.in.xap.tools.operationtool.predicates.machine.MachineWithSameNamePredicate;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsa.GridServiceAgent;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.machine.Machine;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@Component
@Slf4j
public class DefaultShutdownHostService implements ShutdownHostService {

	@Autowired
	@Lazy
	private XapService xapService;

	@Autowired
	private PuRelocateService puRelocateService;

	@Autowired
	private IdExtractor idExtractor;

	/**
	 * enables failfast
	 */
	@Setter
	private boolean forbidWhenOnlyOneHost = true;

	@Setter
	private int maxRelocateAttemptCount = 3;

	@Override
	public void shutdownHost(
			String hostNameOrAddress,
			boolean skipRelocateProcessingUnits,
			boolean skipShutdownAgent,
			Duration demoteMaxSuspendDuration
	) {
		log.info("Asked to shutdown any GSC/GSM/GSA on host {}", hostNameOrAddress);
		final Predicate<Machine> machinePredicate = new MachineWithSameNamePredicate(hostNameOrAddress);

		final Machine[] allMachines = xapService.findAllMachines();
		final Machine[] matchingMachines = Arrays.stream(allMachines).filter(machinePredicate).toArray(Machine[]::new);

		final Set<String> allMachinesNames = idExtractor.extractHostNames(allMachines);
		final Set<String> matchingMachinesNames = idExtractor.extractHostNames(matchingMachines);

		log.info("Found {} machines ({}) in XAP cluster, Found {} matching machines ({}) to shutdown", allMachines.length, allMachinesNames, matchingMachines.length, matchingMachinesNames);

		if (forbidWhenOnlyOneHost) {
			if (matchingMachines.length == allMachines.length) {
				String message = "This will effectively shutdown all Machines in the XAP cluster, this is not supported in order to prevent service interruption";
				log.error(message);
				throw new IllegalStateException(message);
			}
		}

		int foundPuInstanceCount = Integer.MAX_VALUE;

		if (!skipRelocateProcessingUnits) {
			int currentAttemptCount = 0;
			while (currentAttemptCount < maxRelocateAttemptCount && foundPuInstanceCount > 0) {
				currentAttemptCount++;
				//
				foundPuInstanceCount = doRelocate(matchingMachines, machinePredicate, demoteMaxSuspendDuration);
			}
		}

		if (!skipShutdownAgent) {
			final boolean shutdownEvenIfProcessingUnitsInstanceRemains = skipRelocateProcessingUnits;
			Arrays.stream(matchingMachines).forEach(machine -> {
				GridServiceAgent gridServiceAgent = machine.getGridServiceAgent();
				ProcessingUnitInstance[] processingUnitInstances = machine.getProcessingUnitInstances();
				log.info("Found {} ProcessingUnitInstance¨running on Machine {}", processingUnitInstances.length, machine.getHostName());
				if (processingUnitInstances.length == 0 || shutdownEvenIfProcessingUnitsInstanceRemains) {
					log.info("Shutting down GSA {} on Machine {} ...", gridServiceAgent.getUid(), machine.getHostName());
					gridServiceAgent.shutdown();
					log.info("Successfully shut down GSA {} on Machine {}", gridServiceAgent.getUid(), machine.getHostName());
				} else {
					log.warn("Skipped shutdown of GSA {} on Machine {} because {} Processing Unit Instances are still running", gridServiceAgent.getUid(), machine.getHostName(), processingUnitInstances.length);
				}
			});
		} else {
			log.info("Skipped shutdown of GSA as requested by user");
		}

	}

	private int doRelocate(Machine[] matchingMachines, Predicate<Machine> machinePredicate, Duration demoteMaxSuspendDuration) {
		final AtomicInteger foundProcessingUnitsCounter = new AtomicInteger(0);
		Arrays.stream(matchingMachines).forEach(machine -> {

			ProcessingUnitInstance[] processingUnitInstances = machine.getProcessingUnitInstances();
			log.info("Found {} ProcessingUnitInstance¨running on Machine {}", processingUnitInstances.length, machine.getHostName());
			Arrays.stream(processingUnitInstances).forEach(puInstance -> {
				final GridServiceContainer gsc = puInstance.getGridServiceContainer();
				log.info("Processing Unit {} Instance {} is running on GSC {}. Relocating to another GSC ...", puInstance.getName(), puInstance.getId(), gsc.getId());
				foundProcessingUnitsCounter.incrementAndGet();

				try {
					puRelocateService.relocatePuInstance(puInstance, new NotPredicate<>(machinePredicate), true, true, demoteMaxSuspendDuration, false);
				} catch (RuntimeException e) {
					// if there is a failure on 1 PU, maybe other PUs can be relocated, so we continue
					// this exception needs to be catched in order to be able to proceed on other PUs if any
					log.error("Failure while trying to relocate PU instance", e);
				}
			});
		});
		return foundProcessingUnitsCounter.get();
	}

}
