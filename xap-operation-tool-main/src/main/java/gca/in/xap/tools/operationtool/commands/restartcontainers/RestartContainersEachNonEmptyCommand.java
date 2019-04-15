package gca.in.xap.tools.operationtool.commands.restartcontainers;

import gca.in.xap.tools.operationtool.predicates.container.IsNonEmptyContainerPredicate;
import gca.in.xap.tools.operationtool.service.RestartStrategy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "restart-containers-each-non-empty")
public class RestartContainersEachNonEmptyCommand extends AbstractRestartContainersCommand {

	private static final RestartStrategy restartStrategy = defaultIntervalRestartStrategy;

	public RestartContainersEachNonEmptyCommand() {
		super(new IsNonEmptyContainerPredicate(), restartStrategy);
	}

}
