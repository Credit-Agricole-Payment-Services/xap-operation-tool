package gca.in.xap.tools.operationtool.commands.restartcontainers;

import gca.in.xap.tools.operationtool.predicates.container.IsEmptyContainerPredicate;
import gca.in.xap.tools.operationtool.service.RestartStrategy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "restart-containers-empty-only")
public class RestartContainersEmptyOnlyCommand extends AbstractRestartContainersCommand {

	private static final RestartStrategy restartStrategy = noIntervalRestartStrategy;

	public RestartContainersEmptyOnlyCommand() {
		super(new IsEmptyContainerPredicate(), restartStrategy);
	}

}

