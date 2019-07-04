package gca.in.xap.tools.operationtool.service.rebalance;

import com.google.common.util.concurrent.AtomicLongMap;
import lombok.Data;

import java.util.Map;
import java.util.function.Predicate;

@Data
public class MinAndMax<T> {

	private final Map.Entry<T, Long> min;

	private final Map.Entry<T, Long> max;

	public static <T> MinAndMax<T> findMinAndMax(AtomicLongMap<T> atomicLongMap) {
		return findMinAndMax(atomicLongMap, s -> true);
	}

	public static <T> MinAndMax<T> findMinAndMax(AtomicLongMap<T> atomicLongMap, Predicate<T> keysPredicate) {
		if (atomicLongMap.isEmpty()) {
			return null;
		}
		Map.Entry<T, Long> min = null;
		Map.Entry<T, Long> max = null;
		for (Map.Entry<T, Long> entry : atomicLongMap.asMap().entrySet()) {
			if (keysPredicate.test(entry.getKey())) {
				if (min == null) {
					min = entry;
				} else {
					if (entry.getValue() < min.getValue()) {
						min = entry;
					}
				}
				if (max == null) {
					max = entry;
				} else {
					if (entry.getValue() > max.getValue()) {
						max = entry;
					}
				}
			}
		}
		return new MinAndMax<>(min, max);
	}

}
