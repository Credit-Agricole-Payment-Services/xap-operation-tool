package gca.in.xap.tools.operationtool.commands.restartmanagers;

import gca.in.xap.tools.operationtool.service.restartstrategy.RestartStrategy;
import gca.in.xap.tools.operationtool.service.restartstrategy.SequentialRestartStrategy;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsm.GridServiceManager;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.time.Duration;

@Slf4j
@Component
@CommandLine.Command(name = "restart-managers-one-by-one")
public class RestartManagersOneByOneCommand extends AbstractRestartManagersCommand {

	private static final String defaultIntervalDuration = "PT1M";

	@CommandLine.Option(names = "--intervalDuration", defaultValue = defaultIntervalDuration, description = "Interval between each component to restart. Will wait for this interval between each component, to reduce the risk to stress the system when restarting component to quickly. Duration is expressed in ISO_8601 format (example : PT30S for a duration of 30 seconds, PT2M for a duration of 2 minutes). Default value is : " + defaultIntervalDuration)
	private String intervalDuration;

	public RestartManagersOneByOneCommand() {
		super(gsm -> true);
	}

	@Override
	protected RestartStrategy<GridServiceManager> createRestartStrategy() {
		return new SequentialRestartStrategy<>(Duration.parse(intervalDuration));
	}

}
