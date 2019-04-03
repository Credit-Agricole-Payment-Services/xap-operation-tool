package gca.in.xap.tools.operationtool.userinput;

import gca.in.xap.tools.operationtool.util.ConfigAndSecretsHolder;
import gca.in.xap.tools.operationtool.util.SecretsMap;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class SecretsConfigInteractiveCallback {

	private static final String secretContextPropertyPlaceholderValue = "****secret****";

	private final SecretsMap<String, String> providedSecrets = new SecretsMap<>();

	public ConfigAndSecretsHolder requestForSecrets(ConfigAndSecretsHolder configAndSecretsHolder) throws IOException {
		final ConfigAndSecretsHolder holder1 = requestForSecrets(configAndSecretsHolder.getConfigMap(), false);
		final ConfigAndSecretsHolder holder2 = requestForSecrets(configAndSecretsHolder.getSecretsMap(), true);
		return ConfigAndSecretsHolder.merge(holder1, holder2);
	}

	public ConfigAndSecretsHolder requestForSecrets(Map<String, String> inputMap) throws IOException {
		return requestForSecrets(inputMap, false);
	}

	public ConfigAndSecretsHolder requestForSecrets(Map<String, String> inputMap, boolean markEveryThinkgsAsSecret) throws IOException {
		final TreeMap<String, String> configMap = new TreeMap<>();
		final SecretsMap<String, String> secretsMap = new SecretsMap<>();
		//
		try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
			for (Map.Entry<String, String> entry : inputMap.entrySet()) {
				final String propertyName = entry.getKey();
				final String originalPropertyValue = entry.getValue();
				//log.info("Original Property Value for {} : {}", propertyName, originalPropertyValue);
				if (shouldRequestForUserCallback(propertyName, originalPropertyValue)) {
					String inputPropertValue;
					if (providedSecrets.containsKey(propertyName)) {
						inputPropertValue = providedSecrets.get(propertyName);
					} else {
						String promptMessage = "Please enter context property value for '" + propertyName + "' : ";
						log.info(promptMessage);
						System.out.print(promptMessage);
						inputPropertValue = br.readLine();
						//
						providedSecrets.put(propertyName, inputPropertValue);
					}
					log.info("Using context property value for '" + propertyName + "' : '" + inputPropertValue + "'");
					secretsMap.put(propertyName, inputPropertValue);
				} else {
					if (markEveryThinkgsAsSecret || isProbablyAsSecret(propertyName)) {
						secretsMap.put(propertyName, originalPropertyValue);
					} else {
						configMap.put(propertyName, originalPropertyValue);
					}
				}
			}
		}
		return new ConfigAndSecretsHolder(Collections.unmodifiableSortedMap(configMap), secretsMap);
	}

	private boolean isProbablyAsSecret(String propertyName) {
		return propertyName.toLowerCase(Locale.ENGLISH).contains("password");
	}

	private boolean shouldRequestForUserCallback(String propertyName, String originalPropertyValue) {
		return secretContextPropertyPlaceholderValue.equals(originalPropertyValue);
	}

}
