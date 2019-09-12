package gca.in.xap.tools.operationtool.service;

import gca.in.xap.tools.operationtool.predicates.space.IsPrimarySpaceInstancePredicate;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceInstance;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class DefaultDemoteService {

	@Setter
	private int maxSuspendTimeInSeconds = 30;

	private int additionalGracePeriodForBackupToSwitchToPrimary = 30;

	public void demotePrimarySpaceInstances(GridServiceContainer gsc) {
		ProcessingUnitInstance[] processingUnitInstances = gsc.getProcessingUnitInstances();
		for (ProcessingUnitInstance puInstance : processingUnitInstances) {
			demotePrimarySpaceInstances(puInstance);
		}
	}

	public void demotePrimarySpaceInstances(ProcessingUnitInstance puInstance) {
		final String puInstanceName = puInstance.getName();
		final SpaceInstance[] spaceInstances = puInstance.getSpaceInstances();
		final IsPrimarySpaceInstancePredicate isPrimarySpaceInstancePredicate = new IsPrimarySpaceInstancePredicate();

		AtomicInteger demotedSpaceInstancesCount = new AtomicInteger(0);
		for (SpaceInstance spaceInstance : spaceInstances) {
			String spaceInstanceId = spaceInstance.getId();
			if (isPrimarySpaceInstancePredicate.test(spaceInstance)) {
				try {
					log.info("SpaceInstance {} of PU instance {} is Primary. Demoting it allowing max suspend time of {} seconds ...", spaceInstanceId, puInstanceName, maxSuspendTimeInSeconds);
					Future<?> demoteFuture = spaceInstance.demote(maxSuspendTimeInSeconds, TimeUnit.SECONDS);
					Object demoteResult = demoteFuture.get();
					demotedSpaceInstancesCount.incrementAndGet();
					log.info("demoteResult = {}", demoteResult);
				} catch (InterruptedException | ExecutionException e) {
					throw new RuntimeException("Failed to demotePrimarySpaceInstances SpaceInstance " + spaceInstanceId, e);
				}
			}
		}

		if (demotedSpaceInstancesCount.get() > 0) {
			log.info("{} SpaceInstances were demoted. Waiting for {} seconds in order to let the system stabilize ...", demotedSpaceInstancesCount.get(), additionalGracePeriodForBackupToSwitchToPrimary);
			try {
				TimeUnit.SECONDS.sleep(additionalGracePeriodForBackupToSwitchToPrimary);
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted while waiting for a graceful start of the backup", e);
			}
		}
	}

}
