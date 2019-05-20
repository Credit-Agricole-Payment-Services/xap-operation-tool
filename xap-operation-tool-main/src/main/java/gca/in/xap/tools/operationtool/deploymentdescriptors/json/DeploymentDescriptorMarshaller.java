package gca.in.xap.tools.operationtool.deploymentdescriptors.json;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import gca.in.xap.tools.operationtool.deploymentdescriptors.DeploymentDescriptor;
import gca.in.xap.tools.operationtool.service.ObjectMapperFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

@Slf4j
@Component
public class DeploymentDescriptorMarshaller {

	private final ObjectMapper objectMapper;

	private final DefaultPrettyPrinter prettyPrinter;

	public DeploymentDescriptorMarshaller() {
		objectMapper = new ObjectMapperFactory()
				.createObjectMapper();

		DefaultPrettyPrinter.Indenter indenter =
				new DefaultIndenter("  ", DefaultIndenter.SYS_LF);

		prettyPrinter = new DefaultPrettyPrinter();
		// prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
		prettyPrinter.indentObjectsWith(indenter);
		prettyPrinter.indentArraysWith(indenter);
	}

	public void marshall(DeploymentDescriptor deploymentDescriptor, OutputStream outputStream) throws IOException {
		PrintWriter printWriter = new PrintWriter(outputStream);
		objectMapper.writer(prettyPrinter).writeValue(printWriter, deploymentDescriptor);
		printWriter.flush();
		printWriter.print("\r\n");
		printWriter.flush();
	}

}
