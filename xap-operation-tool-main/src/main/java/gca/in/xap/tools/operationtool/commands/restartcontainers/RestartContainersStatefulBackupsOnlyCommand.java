package gca.in.xap.tools.operationtool.commands.restartcontainers;

import gca.in.xap.tools.operationtool.predicates.container.StatefulBackupsOnlyPredicate;
import gca.in.xap.tools.operationtool.service.RestartStrategy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "restart-containers-stateful-backups-only")
public class RestartContainersStatefulBackupsOnlyCommand extends AbstractRestartContainersCommand {

	private static final RestartStrategy restartStrategy = defaultIntervalRestartStrategy;

	public RestartContainersStatefulBackupsOnlyCommand() {
		super(new StatefulBackupsOnlyPredicate(), restartStrategy);
	}

}
