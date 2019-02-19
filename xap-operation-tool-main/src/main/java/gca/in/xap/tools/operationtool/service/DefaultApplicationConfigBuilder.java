package gca.in.xap.tools.operationtool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import gca.in.xap.tools.operationtool.model.DeploymentDescriptor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.application.ApplicationFileDeployment;
import org.openspaces.admin.application.config.ApplicationConfig;
import org.openspaces.admin.pu.config.ProcessingUnitConfig;
import org.openspaces.admin.pu.config.UserDetailsConfig;
import org.openspaces.admin.pu.topology.ProcessingUnitConfigHolder;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

@Slf4j
@ToString
public class DefaultApplicationConfigBuilder implements ApplicationConfigBuilder {

	@Nullable
	private File deploymentDescriptorsDirectory = new File("./deploymentdescriptors");

	@Nullable
	private File applicationArchiveFileOrDirectory = new File(".");

	@Nullable
	private UserDetailsConfig userDetailsConfig;

	/**
	 * Context Properties that would be applied to every PU
	 */
	@Nullable
	private Properties sharedProperties;

	private final ObjectMapper objectMapper = new ObjectMapperFactory().createObjectMapper();

	public DefaultApplicationConfigBuilder withDeploymentDescriptorsDirectory(File deploymentDescriptorsDirectory) {
		this.deploymentDescriptorsDirectory = deploymentDescriptorsDirectory;
		return this;
	}

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
		if (applicationArchiveFileOrDirectory == null) {
			throw new IllegalArgumentException("applicationArchiveFileOrDirectory is required");
		}
		if (!applicationArchiveFileOrDirectory.isFile() && !applicationArchiveFileOrDirectory.isDirectory()) {
			throw new IllegalArgumentException("must be a valid application File or Directory: " + applicationArchiveFileOrDirectory);
		}
		ApplicationConfig applicationConfig = new ApplicationFileDeployment(applicationArchiveFileOrDirectory).create();
		log.info("applicationConfig = {}", applicationConfig);

		final SecretsConfigBuilder secretsConfigBuilder = new SecretsConfigBuilder();

		Map<String, String> sharedPropertiesAsMap = toMap(sharedProperties);
		try {
			sharedPropertiesAsMap = secretsConfigBuilder.askSecrets(sharedPropertiesAsMap);
		} catch (IOException e) {
			throw new RuntimeException("Exception while asking for user input for secrets value", e);
		}

		final File deploymentDescriptorsDirectoryFile = deploymentDescriptorsDirectory == null ? new File(".") : deploymentDescriptorsDirectory;

		for (ProcessingUnitConfigHolder puConfig : applicationConfig.getProcessingUnits()) {
			configure(secretsConfigBuilder, puConfig, sharedPropertiesAsMap, deploymentDescriptorsDirectoryFile);
		}
		log.info("Created ApplicationConfig for application '{}' composed of : {}",
				applicationConfig.getName(),
				ApplicationConfigHelper.getPuNamesInOrderOfDeployment(applicationConfig));

		return applicationConfig;
	}

	private void configure(
			SecretsConfigBuilder secretsConfigBuilder,
			ProcessingUnitConfigHolder puConfig,
			Map<String, String> sharedPropertiesAsMap,
			File deploymentDescriptorsDirectoryFile
	) {
		final File deploymentDescriptorFile = new File(deploymentDescriptorsDirectoryFile, puConfig.getName() + ".json");
		DeploymentDescriptor deploymentDescriptor;
		if (!deploymentDescriptorFile.exists()) {
			log.warn("Deployment Descriptor File : Not Found : {}", deploymentDescriptorFile.getAbsolutePath());
			deploymentDescriptor = null;
		} else {
			log.info("Loading Deployment Descriptor File : {}", deploymentDescriptorFile.getAbsolutePath());
			try {
				deploymentDescriptor = objectMapper.readValue(deploymentDescriptorFile, DeploymentDescriptor.class);
			} catch (IOException e) {
				throw new RuntimeException("Failure while loading DeploymentDescriptor from File " + deploymentDescriptorFile, e);
			}
			log.info("Deployment Descriptor = {}", deploymentDescriptor);
		}

		puConfig.getContextProperties().putAll(sharedPropertiesAsMap);
		if (deploymentDescriptor != null) {
			Map<String, String> additionalContextProperties = deploymentDescriptor.getContextProperties();
			if (additionalContextProperties != null) {
				try {
					additionalContextProperties = secretsConfigBuilder.askSecrets(additionalContextProperties);
				} catch (IOException e) {
					throw new RuntimeException("Exception while asking for user input for secrets value", e);
				}
				puConfig.getContextProperties().putAll(additionalContextProperties);
			}
		}

		if (userDetailsConfig != null) {
			puConfig.setUserDetails(userDetailsConfig);
		}

		ProcessingUnitConfig processingUnitConfig = puConfig.toProcessingUnitConfig();

		if (deploymentDescriptor != null) {
			DeploymentDescriptor.ServiceLevelAgreement sla = deploymentDescriptor.getSla();
			if (sla != null) {
				processingUnitConfig.setMaxInstancesPerMachine(sla.getMaxInstancesPerMachine());
				processingUnitConfig.setMaxInstancesPerVM(sla.getMaxInstancesPerVM());
				processingUnitConfig.setRequiresIsolation(sla.getRequiresIsolation());
				// TODO : processingUnitConfig.setMaxInstancesPerZoneConfig
			}

			DeploymentDescriptor.Topology topology = deploymentDescriptor.getTopology();
			if (topology != null) {
				processingUnitConfig.setClusterSchema(topology.getSchema());
				if (topology.getInstances() != null) {
					processingUnitConfig.setNumberOfInstances(topology.getInstances());
				}
				if (topology.getPartitions() != null) {
					processingUnitConfig.setNumberOfInstances(topology.getPartitions());
				}
				processingUnitConfig.setNumberOfBackups(topology.getBackupsPerPartition());
			}
		}

		// rename the ProcessingUnit ResourceName
		// in order to remove the artifact version from the name
		// so that the PU can support hot deployment later
		// (by simply overwriting files in the /work/deploy directory on the managers)
		String originalProcessingUnitResourceName = processingUnitConfig.getProcessingUnit();
		String newProcessingUnitResourceName = processingUnitConfig.getName() + "-pu.jar";
		if (deploymentDescriptor != null) {
			newProcessingUnitResourceName = deploymentDescriptor.getResource();
		}
		log.info("originalProcessingUnitResourceName = {}, newProcessingUnitResourceName = {}", originalProcessingUnitResourceName, newProcessingUnitResourceName);
		//processingUnitConfig.setProcessingUnit(newProcessingUnitResourceName);

		log.info("processingUnitConfig = {}", processingUnitConfig);
	}

	private static Map<String, String> toMap(@Nullable Properties properties) {
		Map<String, String> result = new TreeMap<>();
		if (properties != null) {
			for (Map.Entry entry : properties.entrySet()) {
				result.put((String) entry.getKey(), (String) entry.getValue());
			}
		}
		return Collections.unmodifiableMap(result);
	}


}
