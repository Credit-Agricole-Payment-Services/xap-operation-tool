package gca.in.xap.tools.operationtool.commands.restartcontainers;

import gca.in.xap.tools.operationtool.predicates.container.StatefulPrimariesOnlyPredicate;
import gca.in.xap.tools.operationtool.service.RestartStrategy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.time.Duration;

@Component
@CommandLine.Command(name = "restart-containers-stateful-primaries-only")
public class RestartContainersStatefulPrimariesOnlyCommand extends AbstractRestartContainersCommand {

	private static final RestartStrategy restartStrategy = new RestartStrategy(Duration.ofMinutes(1));

	public RestartContainersStatefulPrimariesOnlyCommand() {
		super(new StatefulPrimariesOnlyPredicate(), restartStrategy);
	}

}
