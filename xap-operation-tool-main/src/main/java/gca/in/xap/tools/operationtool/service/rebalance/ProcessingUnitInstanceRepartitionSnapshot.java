package gca.in.xap.tools.operationtool.service.rebalance;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Builder
public class ProcessingUnitInstanceRepartitionSnapshot {

	@Getter
	final ProcessingUnitInstanceBreakdownSnapshot actualTotalCounts;

	@Getter
	final ProcessingUnitInstanceBreakdownSnapshot actualPrimaryCounts;

	@Getter
	final ProcessingUnitInstanceBreakdownSnapshot actualBackupCounts;

	public void removeAllZeros() {
		actualTotalCounts.removeAllZeros();
		actualPrimaryCounts.removeAllZeros();
		actualBackupCounts.removeAllZeros();
	}

}
