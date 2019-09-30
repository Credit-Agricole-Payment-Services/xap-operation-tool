package gca.in.xap.tools.operationtool.service.rebalance;

import javax.annotation.Nullable;
import java.time.Duration;

public interface RebalanceProcessingUnitService {

	void rebalanceProcessingUnit(String processingUnitName, boolean onceOnly, @Nullable ZonesGroups zonesGroups, Duration demoteMaxSuspendDuration);

}
