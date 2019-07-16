package gca.in.xap.tools.operationtool.service.rebalance;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;

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

	public void retainsOnlyZones(Set<String> zones) {
		actualTotalCounts.retainsOnlyZones(zones);
		actualPrimaryCounts.retainsOnlyZones(zones);
		actualBackupCounts.retainsOnlyZones(zones);
	}

}
