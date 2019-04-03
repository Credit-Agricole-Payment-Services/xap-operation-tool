package gca.in.xap.tools.operationtool.commands;

import com.kakawait.spring.boot.picocli.autoconfigure.HelpAwarePicocliCommand;
import gca.in.xap.tools.operationtool.service.XapService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.time.Duration;

@Slf4j
@Component
@CommandLine.Command(name = "trigger-gc")
public class GarbageCollectorCommand extends HelpAwarePicocliCommand implements Runnable {

	@Autowired
	@Lazy
	private XapService xapService;

	@Override
	public void run() {
		xapService.printReportOnContainersAndProcessingUnits();
		xapService.setDefaultTimeout(Duration.ofMinutes(2));
		xapService.triggerGarbageCollectorOnEachGsc();
	}

}
