package gca.in.xap.tools.operationtool.service;

import java.util.TreeMap;

public abstract class TreeMapWithDefaultValue<K, V> extends TreeMap<K, V> {

	public TreeMapWithDefaultValue() {
		super();
	}

	protected abstract V createDefaultValue();

	@Override
	public V get(Object key) {
		V result = super.get(key);
		if (result == null) {
			result = createDefaultValue();
			this.put((K) key, result);
		}
		return result;
	}

}
