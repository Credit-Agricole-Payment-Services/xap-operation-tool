package gca.in.xap.tools.operationtool.service;

import gca.in.xap.tools.operationtool.util.ConfigAndSecretsHolder;
import gca.in.xap.tools.operationtool.util.SecretsMap;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

@Slf4j
public class PropertiesMergeBuilder {

	public static ConfigAndSecretsHolder createFromConvention(File deploymentDescriptorsDirectory) {
		final PropertiesMergeBuilder propertiesMergeBuilder = new PropertiesMergeBuilder();

		// using convention over configuration
		// properties files are discovered by convention
		// instead of being passed as a parameter
		// this simplifies the code and the usage

		File properties1 = new File(deploymentDescriptorsDirectory, "shared-public.properties");
		File properties2 = new File(deploymentDescriptorsDirectory, "shared-secrets.properties");


		final ConfigAndSecretsHolder holder = new ConfigAndSecretsHolder(new TreeMap<>(), new SecretsMap<>());
		//
		propertiesMergeBuilder.addContextPropertiesIfExists(properties1, holder.getConfigMap());
		propertiesMergeBuilder.addContextPropertiesIfExists(properties2, holder.getSecretsMap());
		//
		return holder;
	}


	public void addContextPropertiesIfExists(File propertiesFile, Map<String, String> targetProperties) {
		if (propertiesFile.exists()) {
			addContextProperties(propertiesFile, targetProperties);
		} else {
			log.warn("Properties file not found, ignoring : {}", propertiesFile.getAbsolutePath());
		}
	}

	public PropertiesMergeBuilder addContextProperties(File file, Map<String, String> targetProperties) {
		Properties properties = new Properties();
		try (InputStream inputStream = new FileInputStream(file)) {
			properties.load(inputStream);
		} catch (IOException e) {
			throw new IllegalArgumentException("Bad property file", e);
		}
		return this.addContextProperties(properties, targetProperties);
	}

	public PropertiesMergeBuilder addContextProperties(Properties sourceProperties, Map<String, String> targetProperties) {
		sourceProperties.forEach((key, value) -> {
			targetProperties.put((String) key, (String) value);
		});
		return this;
	}

}
