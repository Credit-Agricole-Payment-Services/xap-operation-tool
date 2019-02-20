package gca.in.xap.tools.operationtool.service;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class SecretsConfigBuilder {

	private static final String secretContextPropertyPlaceholderValue = "****secret****";

	private final Map<String, String> providedSecrets = new TreeMap<>();

	public Map<String, String> askSecrets(Map<String, String> inputMap) throws IOException {
		Map<String, String> result = new TreeMap<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
			for (Map.Entry<String, String> entry : inputMap.entrySet()) {
				final String propertyName = entry.getKey();
				final String originalPropertyValue = entry.getValue();
				//log.info("Original Property Value for {} : {}", propertyName, originalPropertyValue);
				if (secretContextPropertyPlaceholderValue.equals(originalPropertyValue)) {
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
					result.put(propertyName, inputPropertValue);
				} else {
					result.put(propertyName, originalPropertyValue);
				}
			}
		}
		return Collections.unmodifiableMap(result);
	}

}
