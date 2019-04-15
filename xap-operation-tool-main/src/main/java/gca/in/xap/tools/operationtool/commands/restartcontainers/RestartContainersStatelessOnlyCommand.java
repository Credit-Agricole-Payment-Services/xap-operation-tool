package gca.in.xap.tools.operationtool.commands.restartcontainers;

import gca.in.xap.tools.operationtool.predicates.pu.IsStatefulProcessingUnitPredicate;
import gca.in.xap.tools.operationtool.service.RestartStrategy;
import org.openspaces.admin.pu.ProcessingUnitInstance;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.time.Duration;
import java.util.Arrays;

@Component
@CommandLine.Command(name = "restart-containers-stateless-only")
public class RestartContainersStatelessOnlyCommand extends AbstractRestartContainersCommand {

	private static final RestartStrategy restartStrategy = defaultIntervalRestartStrategy;

	public RestartContainersStatelessOnlyCommand() {
		super(gsc -> {
			ProcessingUnitInstance[] processingUnitInstances = gsc.getProcessingUnitInstances();
			// if the GSC is not running any PU, then we do not want to restart it
			if (processingUnitInstances.length == 0) {
				return false;
			}
			// if the GSC is running an EmbeddedSpace, then we do not want to restart it
			// else, it means that we are only running stateless PU(s) in this GSC
			return Arrays.stream(processingUnitInstances).noneMatch(new IsStatefulProcessingUnitPredicate());
		}, restartStrategy);
	}

}
