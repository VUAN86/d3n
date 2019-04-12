package de.ascendro.f4m.service.util;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonElement;
import com.google.inject.Inject;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.NotifySubscriberMessageContent;
import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;
import de.ascendro.f4m.service.event.model.resubscribe.ResubscribeRequest;
import de.ascendro.f4m.service.event.model.resubscribe.ResubscribeSubscriptionsRequest;
import de.ascendro.f4m.service.event.model.subscribe.SubscribeRequest;
import de.ascendro.f4m.service.event.model.unsubscribe.UnsubscribeRequestResponse;
import de.ascendro.f4m.service.event.store.EventSubscription;
import de.ascendro.f4m.service.event.store.EventSubscriptionStore;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.registry.exception.F4MNoServiceRegistrySpecifiedException;
import de.ascendro.f4m.service.registry.exception.F4MServiceConnectionInformationNotFoundException;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;

public class EventServiceClientImpl implements EventServiceClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(EventServiceClientImpl.class);
	
	private static final JsonMessage<? extends JsonMessageContent> END_OF_QUEUE_FLAG = new JsonMessage<>();

	protected final JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool;
	protected final JsonMessageUtil jsonUtil;
	protected final ServiceRegistryClient serviceRegistryClient;
	protected final EventSubscriptionStore eventSubscriptionStore;
	protected final Config config;
	private final LoggingUtil loggingUtil;

	private final BlockingDeque<JsonMessage<? extends JsonMessageContent>> eventServiceMessageQueue = new LinkedBlockingDeque<>();
	private ReentrantLock eventServiceMessageQueueLock = new ReentrantLock();
	private Condition mayContinueProcessingEventServiceMessageQueue = eventServiceMessageQueueLock.newCondition();
	private boolean stop;
	private ExecutorService executor;

	
	@Inject
	public EventServiceClientImpl(JsonWebSocketClientSessionPool jsonWebSocketClientSessionPool, JsonMessageUtil jsonUtil,
			ServiceRegistryClient serviceRegistryClient, EventSubscriptionStore eventSubscriptionStore, Config config, LoggingUtil loggingUtil) {
		this.jsonWebSocketClientSessionPool = jsonWebSocketClientSessionPool;
		this.jsonUtil = jsonUtil;
		this.serviceRegistryClient = serviceRegistryClient;
		this.eventSubscriptionStore = eventSubscriptionStore;
		this.config = config;
		this.loggingUtil = loggingUtil;

		stop = false;
		ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("EventServiceClient-MessageQueue").build();
		executor = Executors.newSingleThreadExecutor(factory);
		executor.execute(this::processEventServiceMessageQueue);
	}
	
	@PreDestroy
	public void stopEventServiceMessageQueueProcessing() {
		stop = true;
		try {
			eventServiceMessageQueue.put(END_OF_QUEUE_FLAG);
		} catch (InterruptedException e) {
			// Restore the interrupted status
			LOGGER.debug("Cleanup interrupted", e);
			Thread.currentThread().interrupt();
		}
	}

	private void processEventServiceMessageQueue() {
		try {
			loggingUtil.saveBasicInformationInThreadContext();
			while (true) {
				// Take from blocking queue => wait for next item to arrive
				JsonMessage<? extends JsonMessageContent> message = eventServiceMessageQueue.take();
				if (message != END_OF_QUEUE_FLAG) {
					processEventServiceMessage(message);
				} else {
					break;
				}
			}
		} catch (InterruptedException e) {
			LOGGER.info("Stopping event service message queue processing");
			Thread.currentThread().interrupt();
		}
		executor.shutdown();
	}

	private void processEventServiceMessage(JsonMessage<? extends JsonMessageContent> message) {
		try {
			LOGGER.debug("Sending message to event service: {}", message);
			ServiceConnectionInformation eventServiceConnInfo = getServiceConnectionInformation();
			if (eventServiceConnInfo == null) {
				throw new F4MServiceConnectionInformationNotFoundException("No event service URI found");
			}
			jsonWebSocketClientSessionPool.sendAsyncMessage(eventServiceConnInfo, message);
		} catch (F4MNoServiceRegistrySpecifiedException | F4MServiceConnectionInformationNotFoundException | F4MIOException e) {
			LOGGER.info("No Event Service is registered within Service Registry, service registry not found or I/O error occurred. Postponing message sending for when it will be available.");
			LOGGER.debug("Could not send message to event service", e);
			if (!stop) {
				eventServiceMessageQueue.addFirst(message);
				waitForEventServiceToBeDiscovered();
			}
		} catch (Exception e) {
			LOGGER.error("Could not send the message to event service", e);
		}
	}

	protected ServiceConnectionInformation getServiceConnectionInformation() {
		return serviceRegistryClient.getServiceConnectionInformation(EventMessageTypes.SERVICE_NAME);
	}
	
	private void waitForEventServiceToBeDiscovered() {
		final long retryDelay = config.getPropertyAsLong(F4MConfigImpl.EVENT_SERVICE_DISCOVERY_RETRY_DELAY);
		eventServiceMessageQueueLock.lock();
		try {
			// Wait for either registry getting connected, another event arriving, or retry timeout expiring
			LOGGER.debug("Waiting for event service to be available or retry timeout in " + retryDelay + "ms");
			mayContinueProcessingEventServiceMessageQueue.await(retryDelay, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			eventServiceMessageQueueLock.unlock();
		}
	}
	
	@Override
	public void publish(String topic, JsonElement notificationContent, ZonedDateTime publishDate) {
		publish(topic, false, notificationContent, publishDate);
	}
	
	/* (non-Javadoc)
	 * @see de.ascendro.f4m.service.util.EventServiceUtil#publish(java.lang.String, com.google.gson.JsonElement)
	 */
	@Override
	public void publish(String topic, JsonElement notificationContent) {
		publish(topic, false, notificationContent);
	}
	
	/* (non-Javadoc)
	 * @see de.ascendro.f4m.service.util.EventServiceUtil#publish(java.lang.String, boolean, com.google.gson.JsonElement)
	 */
	@Override
	public void publish(String topic, boolean virtual, JsonElement notificationContent) {
		publish(topic, virtual, notificationContent, null);
	}
	
	@Override
	public void publish(String topic, boolean virtual, JsonElement notificationContent, ZonedDateTime publishDate){
		final JsonMessage<PublishMessageContent> eventPublishMessage = jsonUtil.createNewMessage(EventMessageTypes.PUBLISH);
		final PublishMessageContent publishMessageContent = new PublishMessageContent(topic, virtual);
		publishMessageContent.setNotificationContent(notificationContent);
		if (publishDate != null) {
			publishMessageContent.setPublishDate(publishDate);
		}
		eventPublishMessage.setContent(publishMessageContent);
		sendMessageToEventService(eventPublishMessage);
		
	}

	/* (non-Javadoc)
	 * @see de.ascendro.f4m.service.util.EventServiceUtil#subscribe(boolean, java.lang.String, java.lang.String)
	 */
	@Override
	public void subscribe(boolean virtual, String consumerName, String topic) {
		sendMessageToEventService(jsonUtil.createNewMessage(EventMessageTypes.SUBSCRIBE, new SubscribeRequest(consumerName, topic, virtual)));
	}

	/* (non-Javadoc)
	 * @see de.ascendro.f4m.service.util.EventServiceUtil#unsubscribe(long, java.lang.String)
	 */
	@Override
	public void unsubscribe(long subscription, String topic) {
		eventSubscriptionStore.unregisterSubscription(topic);
		sendMessageToEventService(jsonUtil.createNewMessage(EventMessageTypes.UNSUBSCRIBE, 
				new UnsubscribeRequestResponse(subscription)));
	}

	/* (non-Javadoc)
	 * @see de.ascendro.f4m.service.util.EventServiceUtil#resubscribe(de.ascendro.f4m.service.event.model.resubscribe.ResubscribeSubscriptionsRequest)
	 */
	@Override
	public void resubscribe(ResubscribeSubscriptionsRequest... subscriptions) {
		sendMessageToEventService(jsonUtil.createNewMessage(EventMessageTypes.RESUBSCRIBE, new ResubscribeRequest(subscriptions)));
	}

	/* (non-Javadoc)
	 * @see de.ascendro.f4m.service.util.EventServiceUtil#resubscribe()
	 */
	@Override
	public void resubscribe() {
		Collection<EventSubscription> subscriptions = eventSubscriptionStore.getSubscriptions();
		ResubscribeSubscriptionsRequest[] requests = new ResubscribeSubscriptionsRequest[subscriptions.size()];

		int i = 0;
		for (EventSubscription subscription : subscriptions) {
			Long subscriptionId = subscription.getSubscriptionId();
			if (subscriptionId != null) {
				requests[i++] = new ResubscribeSubscriptionsRequest(subscriptionId, subscription.getConsumerName(), 
						subscription.getTopic(), subscription.isVirtual());
			}
		}

		resubscribe(requests);
	}

	/* (non-Javadoc)
	 * @see de.ascendro.f4m.service.util.EventServiceUtil#notifySubscriber(long, java.lang.String, com.google.gson.JsonElement)
	 */
	@Override
	public void notifySubscriber(long subscription, String topic, JsonElement notificationContent) {
		final JsonMessage<NotifySubscriberMessageContent> notifySubscriberMessage = 
				jsonUtil.createNewMessage(EventMessageTypes.NOTIFY_SUBSCRIBER);
		final NotifySubscriberMessageContent notifySubscriberMessageContent = new NotifySubscriberMessageContent();
		notifySubscriberMessageContent.setSubscription(subscription);
		notifySubscriberMessageContent.setTopic(topic);
		notifySubscriberMessageContent.setNotificationContent(notificationContent);
		notifySubscriberMessage.setContent(notifySubscriberMessageContent);
		sendMessageToEventService(notifySubscriberMessage);
	}
	
	private void sendMessageToEventService(JsonMessage<? extends JsonMessageContent> message) {
		try {
			eventServiceMessageQueue.put(message);
			signalMayContinueProcessingEventServiceMessageQueue();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	/* (non-Javadoc)
	 * @see de.ascendro.f4m.service.util.EventServiceUtil#signalThatEventServiceConnected()
	 */
	@Override
	public void signalThatEventServiceConnected() {
		signalMayContinueProcessingEventServiceMessageQueue();
	}

	/**
	 * Signal that we should retry processing queue, if currently stalled
	 */
	private void signalMayContinueProcessingEventServiceMessageQueue() {
		eventServiceMessageQueueLock.lock();
		try {
			mayContinueProcessingEventServiceMessageQueue.signal();
		} finally {
			eventServiceMessageQueueLock.unlock();
		}
	}
}
