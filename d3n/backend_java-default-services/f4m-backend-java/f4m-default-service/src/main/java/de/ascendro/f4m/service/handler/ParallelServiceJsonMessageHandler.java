package de.ascendro.f4m.service.handler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.di.handler.MessageHandlerProvider;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.session.SessionWrapperFactory;

public class ParallelServiceJsonMessageHandler implements F4MMessageHandler<String> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ParallelServiceJsonMessageHandler.class);
	private static final AtomicInteger poolNumber = new AtomicInteger(1);
	
	private final MessageHandlerProvider<String> messageHandlerProvider;
	private final Config config;
	protected SessionWrapperFactory sessionWrapperFactory;
	private SessionWrapper sessionWrapper;
	
	private final ThreadPoolExecutor executor;
	private final LoggingUtil loggingUtil;

	public ParallelServiceJsonMessageHandler(MessageHandlerProvider<String> messageHandlerProvider, Config config, 
			LoggingUtil loggingUtil, SessionWrapperFactory sessionWrapperFactory) {
		this.messageHandlerProvider = messageHandlerProvider;
		this.config = config;
		this.sessionWrapperFactory = sessionWrapperFactory;
		this.executor = createExecutor();
		this.loggingUtil = loggingUtil;
	}

	protected ThreadPoolExecutor createExecutor() {
		final Integer corePoolSize = config.getPropertyAsInteger(F4MConfigImpl.WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_INITIAL_SIZE);
		final Integer maxPoolSize = config.getPropertyAsInteger(F4MConfigImpl.WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_SIZE);
		final Long keepAliveTime = config.getPropertyAsLong(F4MConfigImpl.WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_KEEP_ALIVE_MS);
		ThreadFactory factory = new ThreadFactoryBuilder()
				.setNameFormat("ParallelServiceJsonMessageHandler-" + poolNumber.getAndIncrement() + "-%d")
				.build();
		final Integer queueSize = config.getPropertyAsInteger(
				F4MConfigImpl.WEBSOCKET_CONNECTION_MESSAGE_HANDLER_THREAD_POOL_MAX_MESSAGE_QUEUE_SIZE);
		return QueuedThreadPoolExecutor.newQueuedThreadPool(corePoolSize, maxPoolSize, queueSize, keepAliveTime, TimeUnit.MILLISECONDS, factory);
	}
	
	protected ExecutorService getExecutor() {
		return executor;
	}
	
	@Override
	public void destroy() {
		executor.shutdown();
	}
	
	@Override
	public void onMessage(String originalMessageEncoded) {
		if (LOGGER.isTraceEnabled()) {
	    	StringBuilder sb = new StringBuilder("Data received:\n")
		        	.append("From: ").append(getSessionWrapper().getTarget())
		        	.append("To: ").append(getSessionWrapper().getSource())
	        		.append("Is client: ").append(getSessionWrapper().isClient())
	        		.append("Message: ").append(originalMessageEncoded);
			LOGGER.trace(sb.toString());
		}
		scheduleOnMessage(originalMessageEncoded);
	}

	/**
	 * Schedules execution of received message processing and return the thread
	 * @param originalMessageEncoded - received message
	 */
	private void scheduleOnMessage(String originalMessageEncoded) {
		executor.execute(() -> {
			loggingUtil.saveBasicInformationInThreadContext();
			executeOnMessage(originalMessageEncoded);
		});
	}

	protected void executeOnMessage(String originalMessageEncoded) {
		final F4MMessageHandler<String> messageHandler = messageHandlerProvider.get();
		messageHandler.setSessionWrapper(getSessionWrapper());
		messageHandler.onMessage(originalMessageEncoded);
	}

	@Override
	public SessionWrapper getSessionWrapper() {
		return sessionWrapper;
	}

	@Override
	public void setSession(Session session) {
		this.sessionWrapper = sessionWrapperFactory.create(session);
	}
	
	@Override
	public void setSessionWrapper(SessionWrapper sessionWrapper) {
		this.sessionWrapper = sessionWrapper;
	}

}
