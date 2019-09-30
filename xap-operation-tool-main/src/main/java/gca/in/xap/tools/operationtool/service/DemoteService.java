package gca.in.xap.tools.operationtool.service;

import lombok.NonNull;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.openspaces.admin.pu.ProcessingUnitInstance;

import java.time.Duration;

public interface DemoteService {

	void demotePrimarySpaceInstances(@NonNull GridServiceContainer gsc, @NonNull Duration demoteMaxSuspendDuration);

	void demotePrimarySpaceInstances(@NonNull ProcessingUnitInstance puInstance, @NonNull Duration demoteMaxSuspendDuration);

}
