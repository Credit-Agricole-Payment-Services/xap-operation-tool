package gca.in.xap.tools.operationtool.service;

import gca.in.xap.tools.operationtool.MaxInstantPerZoneConfigParser;
import gca.in.xap.tools.operationtool.deploymentdescriptors.DeploymentDescriptor;
import gca.in.xap.tools.operationtool.deploymentdescriptors.json.DeploymentDescriptorUnmarshaller;
import gca.in.xap.tools.operationtool.userinput.SecretsConfigInteractiveCallback;
import gca.in.xap.tools.operationtool.util.ConfigAndSecretsHolder;
import gca.in.xap.tools.operationtool.util.MergeMap;
import gca.in.xap.tools.operationtool.util.ZipUtil;
import gca.in.xap.tools.operationtool.util.tempfiles.TempFilesUtils;
import lombok.Builder;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.openspaces.admin.application.ApplicationFileDeployment;
import org.openspaces.admin.application.config.ApplicationConfig;
import org.openspaces.admin.pu.config.ProcessingUnitConfig;
import org.openspaces.admin.pu.config.UserDetailsConfig;
import org.openspaces.admin.pu.topology.ProcessingUnitConfigHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@ToString
@Builder
public class DefaultApplicationConfigBuilder implements ApplicationConfigBuilder {

	@Nullable
	private File applicationArchiveFileOrDirectory;

	@Nullable
	private File deploymentDescriptorsDirectory;

	@Nullable
	private UserDetailsConfig userDetailsConfig;

	/**
	 * Context Properties that would be applied to every PU
	 */
	@Nullable
	private ConfigAndSecretsHolder sharedProperties;

	@Nonnull
	private DeploymentDescriptorUnmarshaller deploymentDescriptorUnmarshaller;

	@Override
	public ApplicationConfig loadApplicationConfig(Predicate<String> procesingUnitNamesPredicates) {
		log.info("applicationArchiveFileOrDirectory = {}", applicationArchiveFileOrDirectory);
		log.info("deploymentDescriptorsDirectory = {}", deploymentDescriptorsDirectory);
		//
		if (applicationArchiveFileOrDirectory == null) {
			throw new IllegalArgumentException("applicationArchiveFileOrDirectory is required");
		}
		if (!applicationArchiveFileOrDirectory.isFile() && !applicationArchiveFileOrDirectory.isDirectory()) {
			throw new IllegalArgumentException("must be a valid application File or Directory: " + applicationArchiveFileOrDirectory);
		}

		final File deploymentDescriptorsDirectoryFile = deploymentDescriptorsDirectory == null ? new File(".") : deploymentDescriptorsDirectory;
		log.info("deploymentDescriptorsDirectoryFile = {}", deploymentDescriptorsDirectoryFile.getAbsolutePath());
		if (!deploymentDescriptorsDirectoryFile.exists() || !deploymentDescriptorsDirectoryFile.isDirectory()) {
			throw new IllegalArgumentException("deploymentDescriptorsDirectoryFile should be a valid directory : " + deploymentDescriptorsDirectoryFile);
		}

		File puDirectory;
		if (applicationArchiveFileOrDirectory.isFile()) {
			puDirectory = TempFilesUtils.getSingleton().createTempDirectory("app_", "_unzipped");
			ZipUtil.unzip(applicationArchiveFileOrDirectory, puDirectory);
		} else {
			puDirectory = applicationArchiveFileOrDirectory;
		}

		final ApplicationConfig applicationConfig = createApplicationConfig(puDirectory);

		// do not print the ApplicationConfig to logs, in order to avoid leaking sensible configuration (passwords)
		//log.debug("applicationConfig = {}", applicationConfig);

		final SecretsConfigInteractiveCallback secretsConfigInteractiveCallback = new SecretsConfigInteractiveCallback();

		final ConfigAndSecretsHolder sharedPropertiesHolder;
		try {
			sharedPropertiesHolder = secretsConfigInteractiveCallback.requestForSecrets(sharedProperties);
		} catch (IOException e) {
			throw new RuntimeException("Exception while asking for user input for secrets value", e);
		}

		for (ProcessingUnitConfigHolder puConfig : applicationConfig.getProcessingUnits()) {
			String puName = puConfig.getName();
			if (!procesingUnitNamesPredicates.test(puName)) {
				log.info("Skipping Processing Unit {} as requested by user", puName);
			} else {
				configure(secretsConfigInteractiveCallback, puConfig, sharedPropertiesHolder, deploymentDescriptorsDirectoryFile, puDirectory);
			}
		}
		log.info("Created ApplicationConfig for application '{}' composed of : {}",
				applicationConfig.getName(),
				ApplicationConfigHelper.getPuNamesInOrderOfDeployment(applicationConfig));

		return applicationConfig;
	}

	private ApplicationConfig createApplicationConfig(File puDirectory) {
		final ApplicationConfig applicationConfig = new ApplicationFileDeployment(puDirectory).create();

		return applicationConfig;
	}

	private void configure(
			SecretsConfigInteractiveCallback secretsConfigInteractiveCallback,
			ProcessingUnitConfigHolder puConfig,
			ConfigAndSecretsHolder sharedPropertiesHolder,
			File deploymentDescriptorsDirectoryFile,
			File puDirectory
	) {
		final File deploymentDescriptorFile = new File(deploymentDescriptorsDirectoryFile, puConfig.getName() + ".json");
		final DeploymentDescriptor deploymentDescriptor = deploymentDescriptorUnmarshaller.parseFile(deploymentDescriptorFile);

		final Map<String, Object> originalContextProperties = Collections.unmodifiableMap(puConfig.getContextProperties());

		final List<Map<String, Object>> contextPropertiesList = new ArrayList<>();

		contextPropertiesList.add(originalContextProperties);
		contextPropertiesList.add(sharedPropertiesHolder.getConfigMap());
		contextPropertiesList.add(sharedPropertiesHolder.getSecretsMap());

		if (deploymentDescriptor != null) {
			final Map<String, Object> additionalContextProperties = deploymentDescriptor.getContextProperties();
			if (additionalContextProperties != null) {
				if (!additionalContextProperties.isEmpty()) {
					ConfigAndSecretsHolder additionalContextPropertiesHolder;
					try {
						additionalContextPropertiesHolder = secretsConfigInteractiveCallback.requestForSecrets(additionalContextProperties);
					} catch (IOException e) {
						throw new RuntimeException("Exception while asking for user input for secrets value", e);
					}
					contextPropertiesList.add(additionalContextPropertiesHolder.getConfigMap());
					contextPropertiesList.add(additionalContextPropertiesHolder.getSecretsMap());
				}
			}
		}

		final MergeMap<String, Object> finalContextProperties = new MergeMap<>(contextPropertiesList.stream().filter(list -> !list.isEmpty()).collect(Collectors.toList()));
		final Map<String, String> finalContextPropertiesFinal = new HashMap<>();
		for (Map.Entry<String, Object> entry : finalContextProperties.entrySet()) {
			finalContextPropertiesFinal.put(entry.getKey(), String.valueOf(entry.getValue()));
		}
		puConfig.setContextProperties(finalContextPropertiesFinal);

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
				if (sla.getZones() != null) {
					processingUnitConfig.setZones(sla.getZones().toArray(new String[0]));
				}
				if (sla.getMaxInstancesPerZone() != null) {
					processingUnitConfig.setMaxInstancesPerZone(new MaxInstantPerZoneConfigParser().parseMaxInstancePerZone(sla.getMaxInstancesPerZone()));
				}
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
		final String originalProcessingUnitResourceName = processingUnitConfig.getProcessingUnit();
		String newProcessingUnitResourceName = processingUnitConfig.getName() + ".jar";
		if (deploymentDescriptor != null) {
			newProcessingUnitResourceName = deploymentDescriptor.getResource();
		}

		final File newProcessingUnitResourceFile = new File(puDirectory, newProcessingUnitResourceName);

		log.info("originalProcessingUnitResourceName = {}, newProcessingUnitResourceName = {}", originalProcessingUnitResourceName, newProcessingUnitResourceName);
		processingUnitConfig.setProcessingUnit(newProcessingUnitResourceFile.getAbsolutePath());

		File originalProcessingUnitResourceFile = new File(puDirectory, originalProcessingUnitResourceName);
		log.info("Renaming file {} to {} ...", originalProcessingUnitResourceFile.getAbsolutePath(), newProcessingUnitResourceFile.getAbsolutePath());
		originalProcessingUnitResourceFile.renameTo(newProcessingUnitResourceFile);

		if (!newProcessingUnitResourceFile.exists()) {
			log.warn("File {} does not exists", newProcessingUnitResourceFile.getAbsolutePath());
		}

		log.debug("processingUnitConfig = {}", processingUnitConfig);
		log.debug("processingUnitConfig (reflection) = {}", ToStringBuilder.reflectionToString(processingUnitConfig));

		// call early the toDeploymentOptions() method in order to fail fast, not waiting for the deploy task to execute
		// this method is called internally during in the deploy method
		String[] deploymentOptions = processingUnitConfig.toDeploymentOptions();
		// avoid printing passwords to logs
		String deploymentOptionsString = Arrays.stream(deploymentOptions).filter(s -> !s.contains("password")).collect(Collectors.joining("\n"));
		log.info("deploymentOptionsString = {}", deploymentOptionsString);
	}

}
