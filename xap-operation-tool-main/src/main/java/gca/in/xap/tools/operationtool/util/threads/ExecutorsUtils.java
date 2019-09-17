package gca.in.xap.tools.operationtool.util.threads;

import lombok.NonNull;

import java.util.concurrent.*;

public class ExecutorsUtils {

	public static ExecutorService newCachedThreadPool(int maxThreadsCount, @NonNull ThreadFactory threadFactory) {
		return new ThreadPoolExecutor(0, maxThreadsCount,
				60L, TimeUnit.SECONDS,
				new SynchronousQueue<>(),
				threadFactory);
	}

}
