package gca.in.xap.tools.operationtool.service;

import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.application.ApplicationFileDeployment;
import org.openspaces.admin.application.config.ApplicationConfig;
import org.openspaces.admin.pu.config.ProcessingUnitConfig;
import org.openspaces.admin.pu.config.UserDetailsConfig;
import org.openspaces.admin.pu.topology.ProcessingUnitConfigHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

@Slf4j
public class DefaultApplicationConfigBuilder implements ApplicationConfigBuilder {

	@Nonnull
	private File applicationArchiveFileOrDirectory;

	@Nullable
	private UserDetailsConfig userDetailsConfig;

	@Nullable
	private Properties sharedProperties;

	public DefaultApplicationConfigBuilder withUserDetailsConfig(UserDetailsConfig userDetails) {
		this.userDetailsConfig = userDetails;
		return this;
	}

	public DefaultApplicationConfigBuilder withApplicationArchiveFileOrDirectory(File applicationArchiveFileOrDirectory) {
		this.applicationArchiveFileOrDirectory = applicationArchiveFileOrDirectory;
		return this;
	}

	public DefaultApplicationConfigBuilder withSharedProperties(Properties sharedProperties) {
		this.sharedProperties = sharedProperties;
		return this;
	}

	@Override
	public ApplicationConfig create() {
		log.info("applicationArchiveFileOrDirectory = {}", applicationArchiveFileOrDirectory);
		log.info("sharedProperties = {}", sharedProperties);
		//
		if (!applicationArchiveFileOrDirectory.isFile() && !applicationArchiveFileOrDirectory.isDirectory()) {
			throw new IllegalArgumentException("must be a valid application File or Directory: " + applicationArchiveFileOrDirectory);
		}
		ApplicationConfig applicationConfig = new ApplicationFileDeployment(applicationArchiveFileOrDirectory).create();
		log.info("applicationConfig = {}", applicationConfig);

		final Map<String, String> sharedPropertiesAsMap = toMap(sharedProperties);

		for (ProcessingUnitConfigHolder puConfig : applicationConfig.getProcessingUnits()) {
			puConfig.getContextProperties().putAll(sharedPropertiesAsMap);
			if (userDetailsConfig != null) {
				puConfig.setUserDetails(userDetailsConfig);
			}
			ProcessingUnitConfig processingUnitConfig = puConfig.toProcessingUnitConfig();
			log.info("processingUnitConfig = {}", processingUnitConfig);
		}

		log.info("Created ApplicationConfig for application '{}' composed of : {}",
				applicationConfig.getName(),
				ApplicationConfigHelper.getPuNamesInOrderOfDeployment(applicationConfig));

		return applicationConfig;
	}

	private static Map<String, String> toMap(@Nullable Properties properties) {
		Map<String, String> result = new TreeMap<>();
		if (properties != null) {
			for (Map.Entry entry : properties.entrySet()) {
				result.put((String) entry.getKey(), (String) entry.getValue());
			}
		}
		return result;
	}

}
