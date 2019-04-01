package gca.in.xap.tools.operationtool.tasks;

import gca.in.xap.tools.operationtool.ApplicationArguments;
import gca.in.xap.tools.operationtool.service.*;
import gca.in.xap.tools.operationtool.userinput.UserConfirmationService;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.application.config.ApplicationConfig;
import org.openspaces.admin.pu.config.UserDetailsConfig;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Slf4j
public class DeployTask {

	private final UserDetailsConfigFactory userDetailsConfigFactory = new UserDetailsConfigFactory();

	private final XapServiceBuilder xapServiceBuilder = new XapServiceBuilder();

	private final UserConfirmationService userConfirmationService = new UserConfirmationService();

	private final ApplicationFileLocator applicationFileLocator = new ApplicationFileLocator();

	public void executeTask(
			ApplicationArguments applicationArguments, boolean wholeMode,
			boolean restartEmptyContainers) throws TimeoutException {

		final @NonNull String archiveFilename = applicationArguments.getApplicationPath();
		final @NonNull String deploymentDescriptorsDirectoryLocation = applicationArguments.getDescriptorsPath();

		final UserDetailsConfig userDetails = userDetailsConfigFactory.createFromUrlEncodedValue(
				applicationArguments.getUsername(),
				applicationArguments.getPassword()
		);

		final XapService xapService = xapServiceBuilder
				.locators(applicationArguments.getLocators())
				.groups(applicationArguments.getGroups())
				.timeout(applicationArguments.getTimeoutDuration())
				.userDetails(userDetails)
				.create();

		final File archiveFileOrDirectory;
		try {
			archiveFileOrDirectory = applicationFileLocator.locateApplicationFile(archiveFilename);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.info("Using archive file : {}", archiveFileOrDirectory);

		final File deploymentDescriptorsDirectory = new File(deploymentDescriptorsDirectoryLocation);

		final PropertiesMergeBuilder propertiesMergeBuilder = PropertiesMergeBuilder.createFromConvention(deploymentDescriptorsDirectory);

		final DefaultApplicationConfigBuilder appDeployBuilder;

		appDeployBuilder = new DefaultApplicationConfigBuilder()
				.withApplicationArchiveFileOrDirectory(archiveFileOrDirectory)
				.withDeploymentDescriptorsDirectory(deploymentDescriptorsDirectory)
				.withUserDetailsConfig(userDetails)
				.withSharedProperties(propertiesMergeBuilder.getMergedProperties());

		ApplicationConfig applicationConfig = appDeployBuilder.create();

		log.info("Will deploy ApplicationConfig : {}", applicationConfig);
		userConfirmationService.askConfirmationAndWait();

		xapService.printReportOnContainersAndProcessingUnits();

		if (wholeMode) {
			xapService.undeployIfExists(applicationConfig.getName());
			xapService.printReportOnContainersAndProcessingUnits();
		}

		if (restartEmptyContainers) {
			xapService.restartEmptyContainers();
		}

		if (wholeMode) {
			xapService.deployWhole(applicationConfig, applicationArguments.getTimeoutDuration());
		} else {
			xapService.deployProcessingUnits(applicationConfig, applicationArguments.getTimeoutDuration(), restartEmptyContainers);
		}

		xapService.printReportOnContainersAndProcessingUnits();
	}


}
