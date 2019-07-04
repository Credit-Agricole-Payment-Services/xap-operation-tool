package gca.in.xap.tools.operationtool.commands.restartcontainers;

import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "restart-containers-all")
public class RestartContainersAllCommand extends AbstractRestartContainersCommand {

	public RestartContainersAllCommand() {
		super(gsc -> true);
	}

}
