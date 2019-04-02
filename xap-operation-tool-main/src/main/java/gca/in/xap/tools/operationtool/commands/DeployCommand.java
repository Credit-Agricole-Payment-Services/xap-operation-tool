package gca.in.xap.tools.operationtool.commands;

import com.kakawait.spring.boot.picocli.autoconfigure.HelpAwarePicocliCommand;
import gca.in.xap.tools.operationtool.XapClientDiscovery;
import gca.in.xap.tools.operationtool.service.*;
import gca.in.xap.tools.operationtool.userinput.UserConfirmationService;
import gca.in.xap.tools.operationtool.util.ConfigAndSecretsHolder;
import gca.in.xap.tools.operationtool.xapauth.XapClientUserDetailsConfigFactory;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.application.config.ApplicationConfig;
import org.openspaces.admin.pu.config.UserDetailsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@CommandLine.Command(name = "deploy")
public class DeployCommand extends HelpAwarePicocliCommand implements Runnable {

	@Autowired
	private XapService xapService;

	@Autowired
	private UserDetailsConfig userDetailsConfig;

	@Autowired
	private UserConfirmationService userConfirmationService;

	@Autowired
	private ApplicationFileLocator applicationFileLocator;

	@Autowired
	private XapClientDiscovery xapClientDiscovery;

	@CommandLine.Option(names = {"--whole"}, description = "Upload the application in whole")
	private boolean wholeMode;

	@CommandLine.Option(names = {"--restartEmptyContainers"}, description = "Restart all GSC that have no running Processing Unit, in order to make mitigate any memory leak")
	private boolean restartEmptyContainers;

	@CommandLine.Option(names = "-f", defaultValue = ".", description = "Path to the File or Directory that contains the application.xml descriptor. Default is current working directory.")
	private String applicationPath;

	@CommandLine.Option(names = "-c", defaultValue = ".", description = "Path to the Directory that contains the deployment descriptors (.json and .properties files). Default is current working directory.")
	private String descriptorsPath;

	@Override
	public void run() {
		final @NonNull String archiveFilename = applicationPath;
		final @NonNull String deploymentDescriptorsDirectoryLocation = descriptorsPath;

		final File archiveFileOrDirectory;
		try {
			archiveFileOrDirectory = applicationFileLocator.locateApplicationFile(archiveFilename);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.info("Using archive file : {}", archiveFileOrDirectory);

		final File deploymentDescriptorsDirectory = new File(deploymentDescriptorsDirectoryLocation);

		final ConfigAndSecretsHolder sharedProperties = PropertiesMergeBuilder.createFromConvention(deploymentDescriptorsDirectory);

		final DefaultApplicationConfigBuilder appDeployBuilder;

		appDeployBuilder = new DefaultApplicationConfigBuilder()
				.withApplicationArchiveFileOrDirectory(archiveFileOrDirectory)
				.withDeploymentDescriptorsDirectory(deploymentDescriptorsDirectory)
				.withUserDetailsConfig(userDetailsConfig)
				.withSharedProperties(sharedProperties);

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

		try {
			if (wholeMode) {
				xapService.deployWhole(applicationConfig, xapClientDiscovery.getTimeoutDuration());
			} else {
				xapService.deployProcessingUnits(applicationConfig, xapClientDiscovery.getTimeoutDuration(), restartEmptyContainers);
			}
		} catch (TimeoutException e) {
			throw new RuntimeException(e);
		}

		xapService.printReportOnContainersAndProcessingUnits();
	}


}
