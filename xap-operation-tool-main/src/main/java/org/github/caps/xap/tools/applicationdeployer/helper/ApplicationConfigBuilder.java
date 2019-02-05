package org.github.caps.xap.tools.applicationdeployer.helper;

import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.application.ApplicationFileDeployment;
import org.openspaces.admin.application.config.ApplicationConfig;
import org.openspaces.admin.pu.config.ProcessingUnitConfig;
import org.openspaces.admin.pu.config.UserDetailsConfig;
import org.openspaces.admin.pu.topology.ProcessingUnitConfigHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

@Slf4j
public class ApplicationConfigBuilder {

	private File applicationArchiveFileOrDirectory;

	private final Map<String, String> contextProperties = new HashMap<>();

	private UserDetailsConfig userDetails;

	public ApplicationConfigBuilder withUserDetailsConfig(UserDetailsConfig userDetails) {
		this.userDetails = userDetails;
		return this;
	}

	public ApplicationConfigBuilder withApplicationArchiveFileOrDirectory(File applicationArchiveFileOrDirectory) {
		this.applicationArchiveFileOrDirectory = applicationArchiveFileOrDirectory;
		return this;
	}

	public ApplicationConfig create() {
		log.info("applicationArchiveFileOrDirectory = {}", applicationArchiveFileOrDirectory);
		log.info("contextProperties = {}", contextProperties);
		//
		if (!applicationArchiveFileOrDirectory.isFile() && !applicationArchiveFileOrDirectory.isDirectory()) {
			throw new IllegalArgumentException("must be a valid application File or Directory: " + applicationArchiveFileOrDirectory);
		}
		ApplicationConfig applicationConfig = new ApplicationFileDeployment(applicationArchiveFileOrDirectory).create();
		log.info("applicationConfig = {}", applicationConfig);

		for (ProcessingUnitConfigHolder puConfig : applicationConfig.getProcessingUnits()) {
			if (!contextProperties.isEmpty()) {
				puConfig.getContextProperties().putAll(contextProperties);
			}
			if (userDetails != null) {
				puConfig.setUserDetails(userDetails);
			}
			ProcessingUnitConfig processingUnitConfig = puConfig.toProcessingUnitConfig();
			log.info("processingUnitConfig = {}", processingUnitConfig);
		}

		log.info("Created ApplicationConfig for application '{}' composed of : {}",
				applicationConfig.getName(),
				ApplicationConfigHelper.getPuNamesInOrderOfDeployment(applicationConfig));

		return applicationConfig;
	}

	public ApplicationConfigBuilder addContextProperties(Path... propertyPaths) {
		return this.addContextProperties(Arrays.asList(propertyPaths));
	}

	private ApplicationConfigBuilder addContextProperties(List<Path> propertyPaths) {
		propertyPaths.forEach(this::addContextProperties);
		return this;
	}

	private ApplicationConfigBuilder addContextProperties(Path propertiesFilepath) {
		File file = propertiesFilepath.toFile();
		Properties properties = new Properties();
		try (InputStream inputStream = new FileInputStream(file)) {
			properties.load(inputStream);
		} catch (IOException e) {
			throw new IllegalArgumentException("Bad property file", e);
		}
		return this.addContextProperties(properties);
	}

	private ApplicationConfigBuilder addContextProperties(Map<? extends Object, ? extends Object> props) {
		if (props != null && !props.isEmpty()) {
			props.forEach((k, v) -> contextProperties.put( //
					k == null ? null : String.valueOf(k), //
					v == null ? null : String.valueOf(v) //
			));
		}
		return this;
	}

}
