package gca.in.xap.tools.operationtool.util;

import lombok.Data;

import java.util.SortedMap;
import java.util.TreeMap;

@Data
public class ConfigAndSecretsHolder {

	public static ConfigAndSecretsHolder merge(ConfigAndSecretsHolder holder1, ConfigAndSecretsHolder holder2) {
		final TreeMap<String, String> mergedConfigMap = new TreeMap<>();
		final SecretsMap<String, String> mergedSecretsMap = new SecretsMap<>();
		//
		mergedConfigMap.putAll(holder1.getConfigMap());
		mergedConfigMap.putAll(holder2.getConfigMap());
		//
		mergedSecretsMap.putAll(holder1.getSecretsMap());
		mergedSecretsMap.putAll(holder2.getSecretsMap());
		//
		return new ConfigAndSecretsHolder(mergedConfigMap, mergedSecretsMap);
	}

	private SortedMap<String, String> configMap;

	private SecretsMap<String, String> secretsMap;

	public ConfigAndSecretsHolder() {
		super();
	}

	public ConfigAndSecretsHolder(
			SortedMap<String, String> configMap,
			SecretsMap<String, String> secretsMap
	) {
		super();
		this.configMap = configMap;
		this.secretsMap = secretsMap;
	}

}
