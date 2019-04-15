package gca.in.xap.tools.operationtool.commands.restartcontainers;

import gca.in.xap.tools.operationtool.predicates.container.IsNonEmptyContainerPredicate;
import gca.in.xap.tools.operationtool.service.RestartStrategy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.time.Duration;

@Component
@CommandLine.Command(name = "restart-containers-each-non-empty")
public class RestartContainersEachNonEmptyCommand extends AbstractRestartContainersCommand {
	public RestartContainersEachNonEmptyCommand() {
		super(new IsNonEmptyContainerPredicate(), new RestartStrategy(Duration.ofMinutes(1)));
	}
}
