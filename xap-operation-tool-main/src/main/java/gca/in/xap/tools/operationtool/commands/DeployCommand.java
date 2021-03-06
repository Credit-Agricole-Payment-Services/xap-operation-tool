package gca.in.xap.tools.operationtool.commands;

import gca.in.xap.tools.operationtool.XapClientDiscovery;
import gca.in.xap.tools.operationtool.commandoptions.PuNamesFilteringOptions;
import gca.in.xap.tools.operationtool.deploymentdescriptors.json.DeploymentDescriptorUnmarshaller;
import gca.in.xap.tools.operationtool.predicates.punames.FilterPuNamesPredicate;
import gca.in.xap.tools.operationtool.service.ApplicationFileLocator;
import gca.in.xap.tools.operationtool.service.DefaultApplicationConfigBuilder;
import gca.in.xap.tools.operationtool.service.PropertiesMergeBuilder;
import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.userinput.UserConfirmationService;
import gca.in.xap.tools.operationtool.util.ConfigAndSecretsHolder;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import gca.in.xap.tools.operationtool.util.picoclicommands.DurationTypeConverter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.application.config.ApplicationConfig;
import org.openspaces.admin.pu.config.UserDetailsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

@Slf4j
@Component
@CommandLine.Command(name = "deploy")
public class DeployCommand extends AbstractAppCommand implements Runnable {

	@Autowired
	@Lazy
	private XapService xapService;

	@Autowired
	private UserDetailsConfig userDetailsConfig;

	@Autowired
	private UserConfirmationService userConfirmationService;

	@Autowired
	private ApplicationFileLocator applicationFileLocator;

	@Autowired
	private XapClientDiscovery xapClientDiscovery;

	@Autowired
	private DeploymentDescriptorUnmarshaller deploymentDescriptorUnmarshaller;

	private static final String DEFAULT_TIMEOUT = "PT10M";

	@CommandLine.Option(names = {"--timeout"},
			defaultValue = DEFAULT_TIMEOUT,
			converter = DurationTypeConverter.class,
			description = "Maximum total duration of the deployment. Default value is " + DEFAULT_TIMEOUT
	)
	private Duration timeoutDuration = Duration.parse(DEFAULT_TIMEOUT);

	@CommandLine.Option(names = {"--whole"}, description = "Upload the application in whole.")
	private boolean wholeMode;

	@CommandLine.ArgGroup(exclusive = false)
	private PuNamesFilteringOptions puNamesFilteringOptions;

	@CommandLine.Option(names = {"--restart-empty-containers"}, description = "Restart all GSC that have no running Processing Unit, in order to make mitigate any memory leak")
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

		appDeployBuilder = DefaultApplicationConfigBuilder.builder()
				.applicationArchiveFileOrDirectory(archiveFileOrDirectory)
				.deploymentDescriptorsDirectory(deploymentDescriptorsDirectory)
				.userDetailsConfig(userDetailsConfig)
				.deploymentDescriptorUnmarshaller(deploymentDescriptorUnmarshaller)
				.sharedProperties(sharedProperties)
				.build();

		if (puNamesFilteringOptions == null) {
			puNamesFilteringOptions = new PuNamesFilteringOptions();
		}
		final Predicate<String> processingUnitsPredicate = FilterPuNamesPredicate.createProcessingUnitsPredicate(puNamesFilteringOptions.processingUnitsIncludes, puNamesFilteringOptions.processingUnitsExcludes);
		final ApplicationConfig applicationConfig = appDeployBuilder.loadApplicationConfig(processingUnitsPredicate);

		log.debug("Will deploy ApplicationConfig : {}", applicationConfig);
		userConfirmationService.askConfirmationAndWait();

		xapService.printReportOnContainersAndProcessingUnits();

		if (wholeMode) {
			xapService.undeployApplicationIfExists(applicationConfig.getName());
			xapService.printReportOnContainersAndProcessingUnits();
		}

		if (restartEmptyContainers) {
			xapService.restartEmptyContainers();
		}

		try {
			if (wholeMode) {
				xapService.deployWhole(applicationConfig, timeoutDuration);
			} else {
				xapService.deployProcessingUnits(applicationConfig, processingUnitsPredicate, timeoutDuration, restartEmptyContainers);
			}
		} catch (TimeoutException e) {
			throw new RuntimeException(e);
		}

		xapService.printReportOnContainersAndProcessingUnits();
	}

}
