package gca.in.xap.tools.operationtool.service;

import java.time.Duration;

public interface ShutdownHostService {

	void shutdownHost(String hostname, boolean skipRelocateProcessingUnits, boolean shutdownAgent, Duration demoteMaxSuspendDuration);

}
