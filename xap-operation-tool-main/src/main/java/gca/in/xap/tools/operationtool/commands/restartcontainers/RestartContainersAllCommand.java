package gca.in.xap.tools.operationtool.commands.restartcontainers;

import gca.in.xap.tools.operationtool.service.RestartStrategy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "restart-containers-all")
public class RestartContainersAllCommand extends AbstractRestartContainersCommand {

	private static final RestartStrategy restartStrategy = noIntervalRestartStrategy;

	public RestartContainersAllCommand() {
		super(gsc -> true, restartStrategy);
	}

}
