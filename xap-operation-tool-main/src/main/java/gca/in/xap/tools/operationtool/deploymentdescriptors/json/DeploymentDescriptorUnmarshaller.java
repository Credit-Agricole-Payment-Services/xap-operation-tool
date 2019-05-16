package gca.in.xap.tools.operationtool.deploymentdescriptors.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import gca.in.xap.tools.operationtool.deploymentdescriptors.DeploymentDescriptor;
import gca.in.xap.tools.operationtool.service.ObjectMapperFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class DeploymentDescriptorUnmarshaller {

	private final ObjectMapper objectMapper;

	public DeploymentDescriptorUnmarshaller() {
		objectMapper = new ObjectMapperFactory().createObjectMapper();
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
	}

	public DeploymentDescriptor parseFile(File deploymentDescriptorFile) {
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
		return deploymentDescriptor;
	}

	public DeploymentDescriptor parseInputStream(InputStream deploymentDescriptorInputStream) {
		try {
			return objectMapper.readValue(deploymentDescriptorInputStream, DeploymentDescriptor.class);
		} catch (IOException e) {
			throw new RuntimeException("Failure while loading DeploymentDescriptor from InputStream", e);
		}
	}

}
