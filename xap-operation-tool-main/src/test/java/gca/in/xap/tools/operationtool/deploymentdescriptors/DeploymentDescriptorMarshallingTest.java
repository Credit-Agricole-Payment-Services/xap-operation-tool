package gca.in.xap.tools.operationtool.deploymentdescriptors;

import gca.in.xap.tools.operationtool.deploymentdescriptors.json.DeploymentDescriptorMarshaller;
import gca.in.xap.tools.operationtool.deploymentdescriptors.json.DeploymentDescriptorUnmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

@Slf4j
public class DeploymentDescriptorMarshallingTest {

	@ParameterizedTest(name = "should_unmarshall_descriptor{arguments}")
	@ValueSource(strings = {"deploymentdescriptors-sample01/in-core.json", "deploymentdescriptors-sample01/in-http-io.json"})
	public void should_unmarshall_descriptor(String resourceLocation) {
		DeploymentDescriptorUnmarshaller unmarshaller = new DeploymentDescriptorUnmarshaller();
		DeploymentDescriptor deploymentDescriptor = unmarshaller.parseInputStream(getClass().getClassLoader().getResourceAsStream(resourceLocation));
		log.info("deploymentDescriptor = {}", deploymentDescriptor);
	}

	@ParameterizedTest(name = "should_unmarshall_descriptor_then_marshall{arguments}")
	@ValueSource(strings = {"deploymentdescriptors-sample01/in-core.json", "deploymentdescriptors-sample01/in-http-io.json"})
	public void should_unmarshall_descriptor_then_marshall(String resourceLocation) throws IOException {
		DeploymentDescriptorUnmarshaller unmarshaller = new DeploymentDescriptorUnmarshaller();
		DeploymentDescriptor deploymentDescriptor = unmarshaller.parseInputStream(getClass().getClassLoader().getResourceAsStream(resourceLocation));

		DeploymentDescriptorMarshaller marshaller = new DeploymentDescriptorMarshaller();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		marshaller.marshall(deploymentDescriptor, outputStream);
		String mashallOutput = outputStream.toString("UTF-8").replaceAll("\r\n", "\n") + "\n";
		log.info("mashallOutput = {}", mashallOutput);

		assertEquals(IOUtils.toString(getClass().getClassLoader().getResourceAsStream(resourceLocation)), mashallOutput);
	}

}
