package gca.in.xap.tools.operationtool.service.rebalance;

import javax.annotation.Nullable;

public interface RebalanceProcessingUnitService {

	void rebalanceProcessingUnit(String processingUnitName, boolean onceOnly, @Nullable ZonesGroups zonesGroups);

}
