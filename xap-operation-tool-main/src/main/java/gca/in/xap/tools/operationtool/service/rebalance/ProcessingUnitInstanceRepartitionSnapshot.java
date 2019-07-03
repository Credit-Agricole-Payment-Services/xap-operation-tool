package gca.in.xap.tools.operationtool.service.rebalance;

import lombok.Data;

@Data
public class ProcessingUnitInstanceRepartitionSnapshot {

	final ProcessingUnitInstanceBreakdownSnapshot actualTotalCounts;

	final ProcessingUnitInstanceBreakdownSnapshot actualPrimaryCounts;

	final ProcessingUnitInstanceBreakdownSnapshot actualBackupCounts;

	public void removeAllZeros() {
		actualTotalCounts.removeAllZeros();
		actualPrimaryCounts.removeAllZeros();
		actualBackupCounts.removeAllZeros();
	}

}
