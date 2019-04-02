package gca.in.xap.tools.operationtool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

import java.util.Arrays;

@Slf4j
@SpringBootApplication()
public class Tool {

	public static void main(String[] args) {
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		BuildInfo.printBuildInformation();
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			log.error("Uncaught Exception in Thread {}", t.getName(), e);
			System.exit(1);
		});
		System.setProperty("com.gigaspaces.unicast.interval", "1000,1000");
		log.info("args = {}", Arrays.asList(args));

		SpringApplication.run(Tool.class, args);
	}

}

