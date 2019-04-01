package gca.in.xap.tools.operationtool.util;

import java.util.*;

/**
 * A MergeMap merge the content of several delegates Maps.
 * A MergeMap is readonly.
 */
public class MergeMap<K, V> implements Map<K, V> {

	private final List<Map<K, V>> delegates;

	public MergeMap(Map<K, V>... delegates) {
		this.delegates = Arrays.asList(delegates);
	}

	public MergeMap(List<Map<K, V>> delegates) {
		this.delegates = delegates;
	}

	@Override
	public int size() {
		return delegates.stream().mapToInt(Map::size).sum();
	}

	@Override
	public boolean isEmpty() {
		return delegates.stream().map(Map::isEmpty).reduce(true, (first, second) -> first && second);
	}

	@Override
	public boolean containsKey(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V get(Object key) {
		throw new UnsupportedOperationException("MergeMap can only be iterated over");
	}

	@Override
	public V put(K key, V value) {
		throw new UnsupportedOperationException("MergeMap is readonly");
	}

	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException("MergeMap is readonly");
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException("MergeMap is readonly");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("MergeMap is readonly");
	}

	@Override
	public Set<K> keySet() {
		Set<K> result = new LinkedHashSet<>();
		for (Map<K, V> delegate : delegates) {
			result.addAll(delegate.keySet());
		}
		return result;
	}

	@Override
	public Collection<V> values() {
		List<V> result = new ArrayList<>();
		for (Map<K, V> delegate : delegates) {
			result.addAll(delegate.values());
		}
		return result;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		Set<Entry<K, V>> result = new LinkedHashSet<>();
		for (Map<K, V> delegate : delegates) {
			result.addAll(delegate.entrySet());
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		boolean first = true;
		for (Map delegate : delegates) {
			if (!first) {
				builder.append(",");
			} else {
				first = false;
			}
			builder.append(delegate);
		}
		builder.append("}");
		return builder.toString();
	}

}
