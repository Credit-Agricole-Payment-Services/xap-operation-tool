package gca.in.xap.tools.operationtool.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Data
@Slf4j
public class RestartStrategy {

	private final Duration intervalBetweenEachComponentRestartDuration;

	private final long intervalBetweenEachComponentRestartInMilliseconds;

	public RestartStrategy(Duration intervalBetweenEachComponentRestartDuration) {
		this.intervalBetweenEachComponentRestartDuration = intervalBetweenEachComponentRestartDuration;
		this.intervalBetweenEachComponentRestartInMilliseconds = intervalBetweenEachComponentRestartDuration.toMillis();
	}

	public void waitBetweenComponent() {
		if (intervalBetweenEachComponentRestartInMilliseconds > 0) {
			log.info("Waiting for {} before next container restart", intervalBetweenEachComponentRestartDuration);
			try {
				Thread.sleep(intervalBetweenEachComponentRestartInMilliseconds);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
