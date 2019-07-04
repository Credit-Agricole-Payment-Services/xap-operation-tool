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
		log.info("");
		log.info("Starting execution of command : {}", this.getClass().getName());
		log.info("");
		try {
			this.run();
			return ExitStatus.OK;
		} finally {
			log.info("");
			log.info("Finished execution of command : {}", this.getClass().getName());
			log.info("");
			log.info("Closing Vertx ...");
			vertx.close();
		}
	}

}
