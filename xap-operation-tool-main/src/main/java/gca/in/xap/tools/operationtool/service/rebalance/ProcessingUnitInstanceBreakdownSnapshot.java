package gca.in.xap.tools.operationtool.service.rebalance;

import com.google.common.util.concurrent.AtomicLongMap;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@ToString
@Builder
public class ProcessingUnitInstanceBreakdownSnapshot {

	@Getter
	final AtomicLongMap<String> countByZone;
	@Getter
	final AtomicLongMap<String> countByMachine;
	@Getter
	final AtomicLongMap<String> countByGSC;

	public ProcessingUnitInstanceBreakdownSnapshot createNewWithZeroCounts() {
		return ProcessingUnitInstanceBreakdownSnapshot.builder()
				.countByZone(initAtomicLongMapCounterWithZeroValues(countByZone))
				.countByMachine(initAtomicLongMapCounterWithZeroValues(countByMachine))
				.countByGSC(initAtomicLongMapCounterWithZeroValues(countByGSC))
				.build();
	}

	public void removeAllZeros() {
		//countByZone.removeAllZeros();
		countByMachine.removeAllZeros();
		countByGSC.removeAllZeros();
	}

	public void retainsOnlyZones(Set<String> zones) {
		Set<String> keysToRemove = new HashSet<>(countByZone.asMap().keySet());
		keysToRemove.removeAll(zones);
		for (String key : keysToRemove) {
			countByZone.remove(key);
		}
	}

	private static AtomicLongMap<String> initAtomicLongMapCounterWithZeroValues(AtomicLongMap<String> potentialCounter) {
		final AtomicLongMap<String> newInstance = AtomicLongMap.create();
		for (String key : potentialCounter.asMap().keySet()) {
			newInstance.addAndGet(key, 0);
		}
		return newInstance;
	}

}
