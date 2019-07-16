package gca.in.xap.tools.operationtool.commands.restartcontainers;

import gca.in.xap.tools.operationtool.predicates.container.StatefulPrimariesOnlyPredicate;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "restart-containers-stateful-primaries-only")
public class RestartContainersStatefulPrimariesOnlyCommand extends AbstractRestartContainersCommand {

	public RestartContainersStatefulPrimariesOnlyCommand() {
		super(new StatefulPrimariesOnlyPredicate());
	}

}
