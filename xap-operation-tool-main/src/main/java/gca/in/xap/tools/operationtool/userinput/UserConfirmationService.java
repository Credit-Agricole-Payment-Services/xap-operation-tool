package gca.in.xap.tools.operationtool.userinput;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class UserConfirmationService {

	@Setter
	private int afterThoughtDurationInSeconds = 5;

	public void askConfirmationAndWait() {
		log.info("Are you sure ? You have {} seconds to use CTRL+C to stop if unsure.", afterThoughtDurationInSeconds);
		try {
			TimeUnit.SECONDS.sleep(afterThoughtDurationInSeconds);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}
