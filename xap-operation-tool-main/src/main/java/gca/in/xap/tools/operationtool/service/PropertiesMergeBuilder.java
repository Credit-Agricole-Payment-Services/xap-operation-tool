package gca.in.xap.tools.operationtool.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Slf4j
public class PropertiesMergeBuilder {

	public static PropertiesMergeBuilder createFromConvention() {
		final PropertiesMergeBuilder propertiesMergeBuilder = new PropertiesMergeBuilder();

		// using convention over configuration
		// properties files are discovered by convention
		// instead of being passed as a parameter
		// this simplifies the code and the usage

		Path properties1 = Paths.get("shared-public.properties");
		Path properties2 = Paths.get("shared-secrets.properties");

		//
		propertiesMergeBuilder.addContextPropertiesIfExists(properties1.toFile());
		propertiesMergeBuilder.addContextPropertiesIfExists(properties2.toFile());

		return propertiesMergeBuilder;
	}

	@Getter
	private final Properties mergedProperties = new Properties();

	public PropertiesMergeBuilder addContextProperties(Path... propertyPaths) {
		return this.addContextProperties(Arrays.asList(propertyPaths));
	}

	public PropertiesMergeBuilder addContextProperties(List<Path> propertyPaths) {
		propertyPaths.forEach(this::addContextProperties);
		return this;
	}

	public PropertiesMergeBuilder addContextProperties(Path propertiesFilepath) {
		File file = propertiesFilepath.toFile();
		return addContextProperties(file);
	}

	public PropertiesMergeBuilder addContextProperties(File file) {
		Properties properties = new Properties();
		try (InputStream inputStream = new FileInputStream(file)) {
			properties.load(inputStream);
		} catch (IOException e) {
			throw new IllegalArgumentException("Bad property file", e);
		}
		return this.addContextProperties(properties);
	}

	public void addContextPropertiesIfExists(File propertiesFile) {
		if (propertiesFile.exists()) {
			addContextProperties(propertiesFile);
		} else {
			log.warn("Properties file not found, ignoring : {}", propertiesFile.getAbsolutePath());
		}
	}

	public PropertiesMergeBuilder addContextProperties(Properties properties) {
		properties.forEach(mergedProperties::put);
		return this;
	}

}
