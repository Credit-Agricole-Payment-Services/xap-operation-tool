package gca.in.xap.tools.operationtool.commands.restartcontainers;

import gca.in.xap.tools.operationtool.predicates.container.IsEmptyContainerPredicate;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Component
@CommandLine.Command(name = "restart-containers-empty-only")
public class RestartContainersEmptyOnlyCommand extends AbstractRestartContainersCommand {

	public RestartContainersEmptyOnlyCommand() {
		super(new IsEmptyContainerPredicate());
	}

}

