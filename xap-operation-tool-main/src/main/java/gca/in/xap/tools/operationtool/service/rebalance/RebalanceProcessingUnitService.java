package gca.in.xap.tools.operationtool.service.rebalance;

import gca.in.xap.tools.operationtool.service.RestartStrategy;

public interface RebalanceProcessingUnitService {

	void rebalanceProcessingUnit(String processingUnitName, RestartStrategy restartStrategy);

}
