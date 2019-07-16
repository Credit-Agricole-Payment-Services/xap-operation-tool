package gca.in.xap.tools.operationtool.service.restartstrategy;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Data
@Slf4j
public class SequentialRestartStrategy<T> implements RestartStrategy<T> {

	private final Duration intervalBetweenEachComponentRestartDuration;

	private final long intervalBetweenEachComponentRestartInMilliseconds;

	public SequentialRestartStrategy(Duration intervalBetweenEachComponentRestartDuration) {
		this.intervalBetweenEachComponentRestartDuration = intervalBetweenEachComponentRestartDuration;
		this.intervalBetweenEachComponentRestartInMilliseconds = intervalBetweenEachComponentRestartDuration.toMillis();
	}

	private void waitBetweenComponent() {
		if (intervalBetweenEachComponentRestartInMilliseconds > 0) {
			log.info("Waiting for {} before next container restart", intervalBetweenEachComponentRestartDuration);
			try {
				Thread.sleep(intervalBetweenEachComponentRestartInMilliseconds);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void perform(@NonNull T[] items, @NonNull ItemVisitor<T> itemVisitor) {
		log.info("Interval Between Each Component Restart In Milliseconds is : {}", intervalBetweenEachComponentRestartInMilliseconds);
		boolean firstIteration = true;
		for (T item : items) {
			if (!firstIteration) {
				// we want to wait between each component restart
				// we don't want to wait before first restart, nor after last restart
				waitBetweenComponent();
			}
			itemVisitor.visit(item);
			firstIteration = false;
		}
	}

}
