package gca.in.xap.tools.operationtool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootApplication()
public class Tool {

	public static void main(String[] args) {
		Thread.setDefaultUncaughtExceptionHandler(Tool::uncaughtException);
		SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		BuildInfo.printBuildInformation();
		System.setProperty("com.gigaspaces.unicast.interval", "1000,1000");
		log.info("args = {}", Arrays.asList(args));

		// start the actual application using Spring Boot ...
		SpringApplication.run(Tool.class, args);
	}

	private static void uncaughtException(Thread t, Throwable e) {
		log.error("Uncaught Exception in Thread {}", t.getName(), e);
		try {
			TimeUnit.MILLISECONDS.sleep(100);
		} catch (InterruptedException e1) {
			throw new RuntimeException(e);
		}
		System.exit(1);
	}
}

