package gca.in.xap.tools.operationtool.commands;

import gca.in.xap.tools.operationtool.service.XapService;
import gca.in.xap.tools.operationtool.util.picoclicommands.AbstractAppCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.File;
import java.util.List;

@Slf4j
@Component
@CommandLine.Command(name = "start-new-container")
public class StartNewContainerCommand extends AbstractAppCommand implements Runnable {

	@Autowired
	@Lazy
	private XapService xapService;

	@CommandLine.Parameters(index = "0", arity = "1..*", description = "names of the $XAP_HOME/gsa/gsc*.xml files")
	private List<File> gscXmlFiles;

	@Override
	public void run() {

		gscXmlFiles.stream()
				.forEach(
						file -> {
							log.info("Attempt to process gsa/gsc file {} ...", file);
							xapService.startNewContainer(file);
						}
				);
	}

}
