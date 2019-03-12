package gca.in.xap.tools.operationtool.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Data
@Slf4j
public class RestartStrategy {

	private final long intervalBetweenEachComponentRestartInMilliseconds;

	public RestartStrategy(Duration duration) {
		this.intervalBetweenEachComponentRestartInMilliseconds = duration.toMillis();
	}

	public void waitBetweenComponent() {
		if (intervalBetweenEachComponentRestartInMilliseconds > 0) {
			log.info("Waiting for {} ms before next container restart", intervalBetweenEachComponentRestartInMilliseconds);
			try {
				Thread.sleep(intervalBetweenEachComponentRestartInMilliseconds);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
