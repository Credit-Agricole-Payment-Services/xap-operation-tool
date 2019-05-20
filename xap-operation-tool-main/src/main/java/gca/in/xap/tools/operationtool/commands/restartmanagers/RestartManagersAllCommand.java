package gca.in.xap.tools.operationtool.commands.restartmanagers;

import gca.in.xap.tools.operationtool.service.RestartStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Slf4j
@Component
@CommandLine.Command(name = "restart-managers-all")
public class RestartManagersAllCommand extends AbstractRestartManagersCommand {

	private static final RestartStrategy restartStrategy = noIntervalRestartStrategy;

	public RestartManagersAllCommand() {
		super(gsm -> true, restartStrategy);
	}

}
