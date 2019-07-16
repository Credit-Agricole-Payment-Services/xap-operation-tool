package gca.in.xap.tools.operationtool.commands.restartcontainers;

import gca.in.xap.tools.operationtool.predicates.container.StatefulBackupsOnlyPredicate;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "restart-containers-stateful-backups-only")
public class RestartContainersStatefulBackupsOnlyCommand extends AbstractRestartContainersCommand {

	public RestartContainersStatefulBackupsOnlyCommand() {
		super(new StatefulBackupsOnlyPredicate());
	}

}
