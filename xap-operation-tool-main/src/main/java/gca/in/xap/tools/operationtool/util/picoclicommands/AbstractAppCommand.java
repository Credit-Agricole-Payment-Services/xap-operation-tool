package gca.in.xap.tools.operationtool.util.picoclicommands;

import com.kakawait.spring.boot.picocli.autoconfigure.ExitStatus;
import com.kakawait.spring.boot.picocli.autoconfigure.HelpAwarePicocliCommand;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@Slf4j
public abstract class AbstractAppCommand extends HelpAwarePicocliCommand {

	@Autowired
	@Lazy
	private Vertx vertx;

	public ExitStatus call() throws Exception {
		this.run();
		log.info("Closing Vertx ...");
		vertx.close();
		return ExitStatus.OK;
	}

}
