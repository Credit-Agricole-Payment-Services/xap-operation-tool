package gca.in.xap.tools.operationtool.service;

import gca.in.xap.tools.operationtool.predicates.space.IsPrimarySpaceInstancePredicate;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.openspaces.admin.space.SpaceInstance;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class DefaultDemoteService implements DemoteService {

	private int additionalGracePeriodForBackupToSwitchToPrimary = 30;

	@Override
	public void demotePrimarySpaceInstances(@NonNull GridServiceContainer gsc, @NonNull Duration demoteMaxSuspendDuration) {
		ProcessingUnitInstance[] processingUnitInstances = gsc.getProcessingUnitInstances();
		for (ProcessingUnitInstance puInstance : processingUnitInstances) {
			demotePrimarySpaceInstances(puInstance, demoteMaxSuspendDuration);
		}
	}

	@Override
	public void demotePrimarySpaceInstances(@NonNull ProcessingUnitInstance puInstance, @NonNull Duration demoteMaxSuspendDuration) {
		final String gscId = puInstance.getGridServiceContainer().getId();
		final String puInstanceName = puInstance.getName();
		final SpaceInstance[] spaceInstances = puInstance.getSpaceInstances();
		final IsPrimarySpaceInstancePredicate isPrimarySpaceInstancePredicate = new IsPrimarySpaceInstancePredicate();

		AtomicInteger demotedSpaceInstancesCount = new AtomicInteger(0);
		for (SpaceInstance spaceInstance : spaceInstances) {
			String spaceInstanceId = spaceInstance.getId();
			if (isPrimarySpaceInstancePredicate.test(spaceInstance)) {
				log.info("In GSC {}, on PU {}, the SpaceInstance {} is Primary. Demoting it allowing max suspend time of {} ...", gscId, puInstanceName, spaceInstanceId, demoteMaxSuspendDuration);
				long startTime = System.currentTimeMillis();
				try {
					Future<?> demoteFuture = spaceInstance.demote(demoteMaxSuspendDuration.toMillis(), TimeUnit.MILLISECONDS);
					Object demoteResult = demoteFuture.get();
					demotedSpaceInstancesCount.incrementAndGet();
					long endTime = System.currentTimeMillis();
					long duration = endTime - startTime;
					log.info("Demote finished successfully (result = {}), duration = {} ms", demoteResult, duration);
				} catch (InterruptedException | ExecutionException e) {
					long endTime = System.currentTimeMillis();
					long duration = endTime - startTime;
					throw new RuntimeException("Failure after " + duration + " ms while demoting Primary space instance " + spaceInstanceId, e);
				}
			}
		}

		if (demotedSpaceInstancesCount.get() > 0) {
			log.info("{} SpaceInstances were demoted in GSC {} on PU {}. Waiting for {} seconds in order to let the system stabilize ...", demotedSpaceInstancesCount.get(), gscId, puInstanceName, additionalGracePeriodForBackupToSwitchToPrimary);
			try {
				TimeUnit.SECONDS.sleep(additionalGracePeriodForBackupToSwitchToPrimary);
			} catch (InterruptedException e) {
				throw new RuntimeException("Interrupted while waiting for a graceful start of the backup", e);
			}
		}
	}

}
