package gca.in.xap.tools.operationtool.service;

import lombok.Getter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class PropertiesMergeBuilder {

	@Getter
	private final Properties mergedProperties = new Properties();

	public PropertiesMergeBuilder addContextProperties(Path... propertyPaths) {
		return this.addContextProperties(Arrays.asList(propertyPaths));
	}

	private PropertiesMergeBuilder addContextProperties(List<Path> propertyPaths) {
		propertyPaths.forEach(this::addContextProperties);
		return this;
	}

	private PropertiesMergeBuilder addContextProperties(Path propertiesFilepath) {
		File file = propertiesFilepath.toFile();
		Properties properties = new Properties();
		try (InputStream inputStream = new FileInputStream(file)) {
			properties.load(inputStream);
		} catch (IOException e) {
			throw new IllegalArgumentException("Bad property file", e);
		}
		return this.addContextProperties(properties);
	}

	private PropertiesMergeBuilder addContextProperties(Properties properties) {
		properties.forEach(mergedProperties::put);
		return this;
	}

}
