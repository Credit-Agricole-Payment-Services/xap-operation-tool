package gca.in.xap.tools.operationtool.commands.restartmanagers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Slf4j
@Component
@CommandLine.Command(name = "restart-managers-all")
public class RestartManagersAllCommand extends AbstractRestartManagersCommand {

	public RestartManagersAllCommand() {
		super(gsm -> true);
	}

}
