package gca.in.xap.tools.operationtool;

import com.kakawait.spring.boot.picocli.autoconfigure.ExitStatus;
import com.kakawait.spring.boot.picocli.autoconfigure.HelpAwarePicocliCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

@Slf4j
@Component
@CommandLine.Command
public class MainCommand extends HelpAwarePicocliCommand {

	@CommandLine.Option(names = {"-v", "--version"}, description = "display version info")
	private boolean versionRequested;

	@Override
	public ExitStatus call() {
		if (versionRequested) {
			log.info(BuildInfo.findVersionInfoString());
			return ExitStatus.TERMINATION;
		}
		return ExitStatus.OK;
	}
}
