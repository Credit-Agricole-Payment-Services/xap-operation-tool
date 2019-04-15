package gca.in.xap.tools.operationtool.commands.restartcontainers;

import com.kakawait.spring.boot.picocli.autoconfigure.HelpAwarePicocliCommand;
import gca.in.xap.tools.operationtool.service.RestartStrategy;
import gca.in.xap.tools.operationtool.service.XapService;
import lombok.extern.slf4j.Slf4j;
import org.openspaces.admin.gsc.GridServiceContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@Slf4j
public abstract class AbstractRestartContainersCommand extends HelpAwarePicocliCommand implements Runnable {

	@Autowired
	@Lazy
	private XapService xapService;

	private final Predicate<GridServiceContainer> predicate;

	private final RestartStrategy restartStrategy;

	public AbstractRestartContainersCommand(Predicate<GridServiceContainer> predicate, RestartStrategy restartStrategy) {
		this.predicate = predicate;
		this.restartStrategy = restartStrategy;
	}

	@Override
	public void run() {
		log.info("Report on all GSC :");
		xapService.printReportOnContainersAndProcessingUnits();

		try {
			log.info("Waiting in order to get a cluster state as accurate as possible ...");
			TimeUnit.MILLISECONDS.sleep(2000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		log.info("Report on GSC to restart :");
		xapService.printReportOnContainersAndProcessingUnits(predicate);

		log.info("RestartStrategy is : {}", restartStrategy);

		xapService.restartContainers(predicate, restartStrategy);
	}

}
