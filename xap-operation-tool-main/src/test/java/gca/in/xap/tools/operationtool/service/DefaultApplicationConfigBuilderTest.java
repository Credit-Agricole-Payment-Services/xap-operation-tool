package gca.in.xap.tools.operationtool.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.openspaces.admin.application.config.ApplicationConfig;

import java.io.File;
import java.util.function.Predicate;

@Slf4j
@Ignore
public class DefaultApplicationConfigBuilderTest {

	@Test
	@Ignore
	public void should_build_application_config() {
		final DefaultApplicationConfigBuilder appDeployBuilder;

		File archiveFileOrDirectory = new File(".");
		File deploymentDescriptorsDirectory = new File("src/test/resources/deploymentdescriptors-sample01");

		appDeployBuilder = DefaultApplicationConfigBuilder.builder()
				.applicationArchiveFileOrDirectory(archiveFileOrDirectory)
				.sharedProperties(PropertiesMergeBuilder.createFromConvention(deploymentDescriptorsDirectory))
				.deploymentDescriptorsDirectory(deploymentDescriptorsDirectory)
				.build();
		log.info("appDeployBuilder = {}", appDeployBuilder);

		Predicate<String> procesingUnitNamesPredicates = s -> true;
		ApplicationConfig applicationConfig = appDeployBuilder.loadApplicationConfig(procesingUnitNamesPredicates);
		log.info("applicationConfig = {}", applicationConfig);
	}

}
