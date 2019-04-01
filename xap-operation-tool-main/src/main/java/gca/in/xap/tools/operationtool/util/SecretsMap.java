package gca.in.xap.tools.operationtool.util;

import java.util.Map;
import java.util.TreeMap;

/**
 * A SecretsMap is like a normal TreeMap, just the difference is that it is used to store Secrets.
 * <p>
 * As such, the toString() method should hide the Map's values, for preventing leaking sensitive info to the logs by mistake.
 * <p>
 * Also, having a strong type for this kind of Map, makes it explicit in the code that the content is sensitive.
 */
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
