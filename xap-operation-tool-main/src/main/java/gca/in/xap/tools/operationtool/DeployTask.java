package gca.in.xap.tools.operationtool;

import gca.in.xap.tools.operationtool.service.DefaultApplicationConfigBuilder;
import gca.in.xap.tools.operationtool.service.PropertiesMergeBuilder;
import gca.in.xap.tools.operationtool.service.UserDetailsConfigFactory;
import gca.in.xap.tools.operationtool.service.XapService;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.openspaces.admin.application.config.ApplicationConfig;
import org.openspaces.admin.pu.config.UserDetailsConfig;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeoutException;

public class DeployTask {

	private final UserDetailsConfigFactory userDetailsConfigFactory = new UserDetailsConfigFactory();

	public void executeTask(
			ApplicationArguments applicationArguments, boolean wholeMode,
			boolean restartEmptyContainers) throws TimeoutException {

		applicationArguments.checkMinimalNumberOfCommandLineArgs(1);
		final String archiveFilename = applicationArguments.commandLineArgs.get(0);

		UserDetailsConfig userDetails = userDetailsConfigFactory.createFromUrlEncodedValue(
				applicationArguments.username,
				applicationArguments.password
		);

		XapService xapService = new XapService.Builder()
				.locators(applicationArguments.locators)
				//.groups(applicationArguments.groups)
				.timeout(applicationArguments.timeoutDuration)
				.userDetails(userDetails)
				.create();

		final File archiveFileOrDirectory = new File(archiveFilename);

		final PropertiesMergeBuilder propertiesMergeBuilder = new PropertiesMergeBuilder();

		if (applicationArguments.commandLineArgs.size() > 1) {
			Path path = Paths.get(applicationArguments.commandLineArgs.get(1));
			propertiesMergeBuilder.addContextProperties(path);
		}

		final DefaultApplicationConfigBuilder appDeployBuilder;

		appDeployBuilder = new DefaultApplicationConfigBuilder()
				.withApplicationArchiveFileOrDirectory(archiveFileOrDirectory)
				.withUserDetailsConfig(userDetails)
				.withSharedProperties(propertiesMergeBuilder.getMergedProperties());

		ApplicationConfig applicationConfig = appDeployBuilder.create();

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
