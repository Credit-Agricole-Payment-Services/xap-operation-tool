package gca.in.xap.tools.operationtool;

import gca.in.xap.tools.operationtool.service.*;
import gca.in.xap.tools.operationtool.userinput.UserConfirmationService;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.openspaces.admin.application.config.ApplicationConfig;
import org.openspaces.admin.pu.config.UserDetailsConfig;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

@Slf4j
public class DeployTask {

	private final UserDetailsConfigFactory userDetailsConfigFactory = new UserDetailsConfigFactory();

	private final XapServiceBuilder xapServiceBuilder = new XapServiceBuilder();

	private final UserConfirmationService userConfirmationService = new UserConfirmationService();

	public void executeTask(
			ApplicationArguments applicationArguments, boolean wholeMode,
			boolean restartEmptyContainers) throws TimeoutException {

		applicationArguments.checkMinimalNumberOfCommandLineArgs(1);
		final String archiveFilename = applicationArguments.commandLineArgs.get(0);

		UserDetailsConfig userDetails = userDetailsConfigFactory.createFromUrlEncodedValue(
				applicationArguments.username,
				applicationArguments.password
		);

		XapService xapService = xapServiceBuilder
				.locators(applicationArguments.locators)
				//.groups(applicationArguments.groups)
				.timeout(applicationArguments.timeoutDuration)
				.userDetails(userDetails)
				.create();


		final File archiveFileOrDirectory = new File(archiveFilename);

		final File deploymentDescriptorsDirectory = new File("./deploymentdescriptors");

		final PropertiesMergeBuilder propertiesMergeBuilder = PropertiesMergeBuilder.createFromConvention();

		final DefaultApplicationConfigBuilder appDeployBuilder;

		appDeployBuilder = new DefaultApplicationConfigBuilder()
				.withApplicationArchiveFileOrDirectory(archiveFileOrDirectory)
				.withDeploymentDescriptorsDirectory(deploymentDescriptorsDirectory)
				.withUserDetailsConfig(userDetails)
				.withSharedProperties(propertiesMergeBuilder.getMergedProperties());

		ApplicationConfig applicationConfig = appDeployBuilder.create();

		log.info("Will deploy ApplicationConfig : {}", applicationConfig);
		userConfirmationService.askConfirmationAndWait();

		if (archiveFileOrDirectory.isFile()) {
			File outputDirectory = new File(".");
			unzip(archiveFileOrDirectory, outputDirectory);
		}

		xapService.printReportOnContainersAndProcessingUnits();

		if (wholeMode) {
			xapService.undeployIfExists(applicationConfig.getName());
			xapService.printReportOnContainersAndProcessingUnits();
		}

		if (restartEmptyContainers) {
			xapService.restartEmptyContainers();
		}

		if (wholeMode) {
			xapService.deployWhole(applicationConfig, applicationArguments.timeoutDuration);
		} else {
			xapService.deployProcessingUnits(applicationConfig, applicationArguments.timeoutDuration, restartEmptyContainers);
		}

		xapService.printReportOnContainersAndProcessingUnits();
	}


	public static void unzip(File archiveFile, File destinationDirectory) {
		try {
			ZipFile zipFile = new ZipFile(archiveFile);
			zipFile.extractAll(destinationDirectory.getPath());
		} catch (ZipException e) {
			throw new RuntimeException(e);
		}
	}

}
