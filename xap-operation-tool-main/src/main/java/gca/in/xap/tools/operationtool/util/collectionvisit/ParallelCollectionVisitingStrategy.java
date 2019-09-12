package gca.in.xap.tools.operationtool.util.collectionvisit;

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
public class ParallelCollectionVisitingStrategy<T> implements CollectionVisitingStrategy<T> {

	private final int maxNumberOfThreads = 32;

	@Override
	public void perform(@NonNull T[] items, @NonNull ItemVisitor<T> itemVisitor) {
		// if the number of items is very high, we should not use as much threads, but use a limit
		final int maxThreadsCount = Math.min(maxNumberOfThreads, items.length);

		//
		ExecutorService executor = createExecutorService(maxThreadsCount);

		// submit all tasks
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

		// shutdown all threads
		executor.shutdownNow();
	}

	private ExecutorService createExecutorService(int maxThreadsCount) {
		// the ThreadPool should be large enough
		// in order to execute a task for each machine in the cluster, ideally, at the same time

		return newCachedThreadPool(maxThreadsCount, new ThreadFactory() {

			private final String threadNamePrefix = ParallelCollectionVisitingStrategy.class.getSimpleName();

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

}
