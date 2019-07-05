package gca.in.xap.tools.operationtool.service.rebalance;

import com.google.common.collect.TreeMultimap;
import com.google.common.util.concurrent.AtomicLongMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@ToString
@AllArgsConstructor
@Builder
public class MinAndMax<T extends Comparable<T>> {

	private final Long min;
	private final Long max;

	private final List<T> keysWithMinValue;
	private final List<T> keysWithMaxValue;

	public boolean needsRebalancing() {
		return max != null && min != null && max > min + 1;
	}

	public T getBestKeyOfMin() {
		if (keysWithMinValue == null) {
			return null;
		}
		return keysWithMinValue.iterator().next();
	}

	public T getBestKeyOfMax() {
		if (keysWithMaxValue == null) {
			return null;
		}
		return keysWithMaxValue.iterator().next();
	}

	public static <T extends Comparable<T>> MinAndMax<T> findMinAndMax(@NonNull AtomicLongMap<T> atomicLongMap) {
		return findMinAndMax(atomicLongMap, s -> true);
	}

	@Nullable
	public static <T extends Comparable<T>> MinAndMax<T> findMinAndMax(@NonNull AtomicLongMap<T> atomicLongMap, @NonNull Predicate<T> keysPredicate) {
		final TreeMultimap<Long, T> reverseMap = TreeMultimap.create();
		if (atomicLongMap.isEmpty()) {
			return null;
		}
		for (Map.Entry<T, Long> entry : atomicLongMap.asMap().entrySet()) {
			if (keysPredicate.test(entry.getKey())) {
				reverseMap.put(entry.getValue(), entry.getKey());
			}
		}
		final Map.Entry<Long, Collection<T>> minEntry = reverseMap.asMap().firstEntry();
		final Map.Entry<Long, Collection<T>> maxEntry = reverseMap.asMap().lastEntry();
		return MinAndMax.<T>builder()
				.min(minEntry.getKey())
				.max(maxEntry.getKey())
				.keysWithMaxValue(new ArrayList<>(maxEntry.getValue()))
				.keysWithMinValue(new ArrayList<>(minEntry.getValue()))
				.build();
	}

}
