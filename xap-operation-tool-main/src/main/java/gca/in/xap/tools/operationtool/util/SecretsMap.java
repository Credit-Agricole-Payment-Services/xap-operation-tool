package gca.in.xap.tools.operationtool.util;

import java.util.Map;
import java.util.TreeMap;

public class SecretsMap<K, V> extends TreeMap<K, V> {

	public static final String PLACEHOLDER = "****secret****";

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		boolean first = true;
		for (Map.Entry<K, V> enty : this.entrySet()) {
			if (!first) {
				builder.append(",");
			} else {
				first = false;
			}
			builder.append(enty.getKey()).append("=").append(PLACEHOLDER);
		}
		builder.append("}");
		return builder.toString();
	}

}
