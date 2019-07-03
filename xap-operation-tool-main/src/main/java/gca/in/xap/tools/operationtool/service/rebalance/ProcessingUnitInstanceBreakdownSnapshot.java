package gca.in.xap.tools.operationtool.service.rebalance;

import com.google.common.util.concurrent.AtomicLongMap;
import lombok.Data;

@Data
public class ProcessingUnitInstanceBreakdownSnapshot {

	final AtomicLongMap<String> countByZone;
	final AtomicLongMap<String> countByMachine;
	final AtomicLongMap<String> countByGSC;

	public ProcessingUnitInstanceBreakdownSnapshot createNewWithZeroCounts() {
		return new ProcessingUnitInstanceBreakdownSnapshot(
				initAtomicLongMapCounterWithZeroValues(countByMachine),
				initAtomicLongMapCounterWithZeroValues(countByZone),
				initAtomicLongMapCounterWithZeroValues(countByGSC)
		);
	}

	public void removeAllZeros() {
		//countByZone.removeAllZeros();
		countByMachine.removeAllZeros();
		countByGSC.removeAllZeros();
	}

	private static AtomicLongMap<String> initAtomicLongMapCounterWithZeroValues(AtomicLongMap<String> potentialCounter) {
		final AtomicLongMap<String> newInstance = AtomicLongMap.create();
		for (String key : potentialCounter.asMap().keySet()) {
			newInstance.addAndGet(key, 0);
		}
		return newInstance;
	}

}
