package de.ascendro.f4m.service.handler;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueuedThreadPoolExecutor extends ThreadPoolExecutor {
	private static final Logger LOGGER = LoggerFactory.getLogger(QueuedThreadPoolExecutor.class);
	private QueueingExecutionHandler queueingExecutionHandler;
	private ExecutorService backFeedThreadExecutor;
	private Thread backFeedThread;
	private boolean stop = false;
	
	protected QueuedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, QueueingExecutionHandler queueingExecutionHandler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, queueingExecutionHandler);
		this.queueingExecutionHandler = queueingExecutionHandler;
		backFeedThreadExecutor = Executors.newSingleThreadExecutor(wrappedFactory(threadFactory, "-backFeed"));
		backFeedThreadExecutor.execute(this::startFeedFromRejectionQueueToThreadPool);
	}

	public static QueuedThreadPoolExecutor newQueuedThreadPool(int corePoolSize, int maximumPoolSize,
			int maximumQueueSize, long keepAliveTime, TimeUnit unit, ThreadFactory threadFactory) {
		Validate.isTrue(maximumQueueSize > 1, "Pool secondary queue size must be greater than one: %d", maximumQueueSize);
		//since 1 task will live in startFeedFromRejectionQueueToThreadPool, the queue itself should be smaller
		QueueingExecutionHandler queuePolicy = new QueueingExecutionHandler(maximumQueueSize - 1);
		BlockingQueue<Runnable> workQueue = new SynchronousQueue<>(true);
		return new QueuedThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
				threadFactory, queuePolicy);
	}

	private ThreadFactory wrappedFactory(ThreadFactory threadFactory, String suffix) {
		return r -> {
			Thread newThread = threadFactory.newThread(r);
			newThread.setName(newThread.getName() + suffix);
			return newThread;
		};
	}
	
	private void startFeedFromRejectionQueueToThreadPool() {
		try {
			LOGGER.debug("Backfeeder thread started");
			backFeedThread = Thread.currentThread();
			while (!stop) {
				Runnable task = queueingExecutionHandler.getSecondaryQueue().take();
				LOGGER.trace("Backfeeder took task {} from secondary queue", task);
				getQueue().put(task); 
				LOGGER.trace("Backfeeder put task {} back to executor's queue", task);
			}
		} catch (InterruptedException e) {
			if (stop) {
				LOGGER.debug("Backfeeder stopped via interrupt");
			} else {
				// Restore the interrupted status
				LOGGER.debug("Cleanup interrupted and should have stopped {}", stop, e);
				Thread.currentThread().interrupt();
			}
		} finally {
			LOGGER.debug("Backfeeder thread stopped");
		}
	}
	
	@Override
	public void shutdown() {
		backFeedThreadExecutor.shutdown();
		super.shutdown();
		stop = true;
		int waitingSize = queueingExecutionHandler.getSecondaryQueue().size();
		if (waitingSize > 0) {
			LOGGER.warn("Going to drop execution of {} waiting tasks", waitingSize);
		}
		if (backFeedThread != null) {
			backFeedThread.interrupt(); //killing with an axe is probably not the most correct solution, but it surely works
		}
	}
	
	@Override
	public List<Runnable> shutdownNow() {
		List<Runnable> waiting = new LinkedList<>();
		waiting.addAll(backFeedThreadExecutor.shutdownNow());
		waiting.addAll(super.shutdownNow());
		return waiting;
	}
	
	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		//not exactly correct, because it will take twice as long, but let's not worry too much about it 
		boolean backFeedTerminated = backFeedThreadExecutor.awaitTermination(timeout, unit);
		boolean superPoolTerminated = super.awaitTermination(timeout, unit);
		LOGGER.debug("backFeedTerminated {} and superPoolTerminated {}", backFeedTerminated, superPoolTerminated);
		return backFeedTerminated && superPoolTerminated;
	}
	
	protected static class QueueingExecutionHandler implements RejectedExecutionHandler {
		private static final Logger LOGGER = LoggerFactory.getLogger(QueueingExecutionHandler.class);
		private final BlockingQueue<Runnable> secondaryQueue;

		public QueueingExecutionHandler(int maximumQueueSize) {
			secondaryQueue = new LinkedBlockingQueue<>(maximumQueueSize);
		}

		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			try {
				getSecondaryQueue().put(r);
			} catch (InterruptedException e) {
				LOGGER.debug("Queueing into secondary queue interrupted", e);
				Thread.currentThread().interrupt();
				throw new RejectedExecutionException("Task " + r.toString() + " rejected from " + executor.toString()
					+ " because of full queue (" + getSecondaryQueue().size() + ")");
			}
		}

		public BlockingQueue<Runnable> getSecondaryQueue() {
			return secondaryQueue;
		}
	}
}
