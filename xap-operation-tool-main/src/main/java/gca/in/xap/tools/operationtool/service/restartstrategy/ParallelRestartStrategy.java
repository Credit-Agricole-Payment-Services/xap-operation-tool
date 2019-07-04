package gca.in.xap.tools.operationtool.service.restartstrategy;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static gca.in.xap.tools.operationtool.util.threads.ExecutorsUtils.newCachedThreadPool;

@Data
@Slf4j
public class ParallelRestartStrategy<T> implements RestartStrategy<T> {

	private final ExecutorService executor;

	public ParallelRestartStrategy() {
		// the ThreadPool should be large enough
		// in order to execute a task for each machine in the cluster, ideally, at the same time
		executor = newCachedThreadPool(32, new ThreadFactory() {

			private final String threadNamePrefix = ParallelRestartStrategy.class.getSimpleName();

			private final AtomicInteger counter = new AtomicInteger(0);

			@Override
			public Thread newThread(Runnable runnable) {
				final int threadIndex = counter.incrementAndGet();
				final Thread thread = new Thread(runnable);
				thread.setDaemon(true);
				thread.setName(threadNamePrefix + "-" + String.format("%03d", threadIndex));
				return thread;
			}
		});
	}

	@Override
	public void perform(@NonNull T[] items, @NonNull ItemVisitor<T> itemVisitor) {
		List<Future<?>> results = new ArrayList<>();
		for (T item : items) {
			Future<?> future = executor.submit(() -> itemVisitor.visit(item));
			results.add(future);
		}
		// wait for all tasks to complete
		for (Future<?> future : results) {
			try {
				future.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
