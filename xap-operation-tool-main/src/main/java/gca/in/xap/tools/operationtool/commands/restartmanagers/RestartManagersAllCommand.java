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
@CommandLine.Command(name = "restart-managers-all")
public class RestartManagersAllCommand extends AbstractRestartManagersCommand {

	public RestartManagersAllCommand() {
		super(gsm -> true);
	}

	@Override
	protected RestartStrategy<GridServiceManager> createRestartStrategy() {
		return new SequentialRestartStrategy<>(Duration.ZERO);
	}

}
