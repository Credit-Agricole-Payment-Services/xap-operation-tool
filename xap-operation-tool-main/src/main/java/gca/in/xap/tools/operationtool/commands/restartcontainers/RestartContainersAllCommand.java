package gca.in.xap.tools.operationtool.commands.restartcontainers;

import gca.in.xap.tools.operationtool.service.RestartStrategy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.time.Duration;

@Component
@CommandLine.Command(name = "restart-containers-all")
public class RestartContainersAllCommand extends AbstractRestartContainersCommand {
	public RestartContainersAllCommand() {
		super(gsc -> true, new RestartStrategy(Duration.ZERO));
	}
}
