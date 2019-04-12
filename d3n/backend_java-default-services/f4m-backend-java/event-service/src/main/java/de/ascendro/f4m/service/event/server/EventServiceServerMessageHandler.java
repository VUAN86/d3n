package de.ascendro.f4m.service.event.server;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.config.EventConfig;
import de.ascendro.f4m.service.event.model.info.InfoRequest;
import de.ascendro.f4m.service.event.model.info.InfoResponse;
import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;
import de.ascendro.f4m.service.event.model.resubscribe.ResubscribeRequest;
import de.ascendro.f4m.service.event.model.resubscribe.ResubscribeResponse;
import de.ascendro.f4m.service.event.model.resubscribe.ResubscribeSubscriptionsRequest;
import de.ascendro.f4m.service.event.model.resubscribe.ResubscribeSubscriptionsResponse;
import de.ascendro.f4m.service.event.model.subscribe.SubscribeRequest;
import de.ascendro.f4m.service.event.model.subscribe.SubscribeResponse;
import de.ascendro.f4m.service.event.model.unsubscribe.UnsubscribeRequestResponse;
import de.ascendro.f4m.service.event.pool.EventPool;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.F4MShuttingDownException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.registry.EventServiceStore;
import de.ascendro.f4m.service.session.WebSocketSessionWrapper;

/**
 * Jetty Web Socket message handler for Event Service
 */
public class EventServiceServerMessageHandler extends DefaultJsonMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventServiceServerMessageHandler.class);

	private final EventPool eventPool;

	private EventServiceStore eventServiceStore;

	public EventServiceServerMessageHandler(EventPool eventPool, EventServiceStore eventServiceStore) {
		this.eventPool = eventPool;
		this.eventServiceStore = eventServiceStore;
	}

	@SuppressWarnings("unchecked")
	@Override
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> message)
			throws F4MException {
		final JsonMessageContent resultContent;

		final EventMessageTypes type = message.getType(EventMessageTypes.class);

		switch (type) {
		case INFO:
			resultContent = onInfo((JsonMessage<InfoRequest>) message);
			break;
		case SUBSCRIBE:
			resultContent = onSubscribe((JsonMessage<SubscribeRequest>) message);
			break;
		case UNSUBSCRIBE:
			resultContent = onUnsubscribe((JsonMessage<UnsubscribeRequestResponse>) message);
			break;
		case RESUBSCRIBE:
			resultContent = onResubscribe((JsonMessage<ResubscribeRequest>) message);
			break;
		case PUBLISH:
			resultContent = onPublish((JsonMessage<PublishMessageContent>) message);
			break;
		default:
			throw new F4MValidationFailedException("Event Service message type[" + message.getName()
					+ "] not recognized");
		}
		return resultContent;
	}

	/**
	 *  Event Service info message handling
	 *
	 * @param infoMessage - message to be processed
	 *
	 * @return - InfoResponse content for reply
	 *
	 */
	protected InfoResponse onInfo(JsonMessage<InfoRequest> infoMessage) {
		final InfoResponse response = new InfoResponse();

		response.setJmsPort(config.getPropertyAsInteger(EventConfig.ACTIVE_MQ_PORT));

		return response;
	}

	/**
	 * Event Service subscribe message handling
	 * 
	 * @param subscribeMessage
	 *            - message to be processed
	 * @return SubscribeResponse contents for reply
	 * @throws F4MException
	 *             - failed to subscribe via Event Pool
	 */
	protected SubscribeResponse onSubscribe(JsonMessage<SubscribeRequest> subscribeMessage) throws F4MException {
		verifyStartupStatus(subscribeMessage);
		final SubscribeRequest subscribeMessageContent = subscribeMessage.getContent();
		final String topic = subscribeMessageContent.getTopic();
		if (subscribeMessageContent.isVirtual()) {
			Validate.notBlank(subscribeMessageContent.getConsumerName(), "For virtual queues, consumer name is mandatory");
		}
		try {
			LOGGER.debug("onSubscribe consumerName {} topic {} subscribeMessageContent.isVirtual() {}",
						 subscribeMessageContent.getConsumerName(), topic, subscribeMessageContent.isVirtual());

			final long subscriptionId = eventPool.subscribe(subscribeMessageContent.getConsumerName(),
					topic, subscribeMessageContent.isVirtual(), getSessionWrapper());
			return new SubscribeResponse(subscriptionId, subscribeMessageContent.isVirtual(), subscribeMessageContent.getConsumerName(), topic);
		} catch (F4MShuttingDownException e) {
			return null; // If shutting down, no need to respond => would cause more errors
		}
	}

	private void verifyStartupStatus(JsonMessage<?> subscribeMessage) {
		if (!eventServiceStore.allKnownEventServicesAreConnected()) {
			LOGGER.warn("Not all MQ information is known, message may be lost between nodes {}",
					subscribeMessage.getName());
		}
	}

	/**
	 * Event Service resubscribe message handling
	 * 
	 * @param resubscribeMessage
	 *            - message to be processed
	 * @return ResubscribeResponse contents for reply
	 * @throws F4MException
	 *             - failed to resubscribe via Event Pool
	 */
	protected ResubscribeResponse onResubscribe(JsonMessage<ResubscribeRequest> resubscribeMessage)
			throws F4MException {
		verifyStartupStatus(resubscribeMessage);
		final ResubscribeRequest resubscribeRequest = resubscribeMessage.getContent();

		final List<ResubscribeSubscriptionsResponse> resubscriptions = new ArrayList<>();

		try {
			for (ResubscribeSubscriptionsRequest resubscription : resubscribeRequest.getSubscriptions()) {
				Validate.isTrue(!resubscription.isVirtual() || StringUtils.isNotBlank(resubscription.getConsumerName()));
				final long resubscriptionId = eventPool.resubscribe(resubscription.getSubscription(), resubscription.getConsumerName(),
						resubscription.getTopic(), resubscription.isVirtual(), (WebSocketSessionWrapper) getSessionWrapper());
	
				final ResubscribeSubscriptionsResponse resubscriptionSubscriptionResponse = new ResubscribeSubscriptionsResponse(
						resubscription);
				resubscriptionSubscriptionResponse.setResubscription(resubscriptionId);
				resubscriptions.add(resubscriptionSubscriptionResponse);
			}
			return new ResubscribeResponse(
					resubscriptions.toArray(new ResubscribeSubscriptionsResponse[0]));
		} catch (F4MShuttingDownException e) {
			return null; // If shutting down, no need to respond => would cause more errors
		}
	}

	/**
	 * Event Service unsubscribe message handling
	 * 
	 * @param unsubscribeMessage
	 *            - message to be processed
	 * @return UnsubscribeRequestResponse contents for reply
	 * @throws F4MException
	 *             - failed to unsubscribe via Event Pool
	 */
	protected UnsubscribeRequestResponse onUnsubscribe(JsonMessage<UnsubscribeRequestResponse> unsubscribeMessage)
			throws F4MException {
		verifyStartupStatus(unsubscribeMessage);
		final UnsubscribeRequestResponse unsubscribeMessageContent = unsubscribeMessage.getContent();
		try {
			eventPool.unsubscribe(unsubscribeMessageContent.getSubscription(), getSessionWrapper());
			return unsubscribeMessageContent;
		} catch (F4MShuttingDownException e) {
			return null; // If shutting down, no need to respond => would cause more errors
		}
	}

	/**
	 * Event Service publish message handling
	 * 
	 * @param publishMessage
	 *            - message to be processed
	 * @return PublishMessageContent contents for reply
	 * @throws F4MException
	 *             - failed to publish via Event Pool
	 */
	protected PublishMessageContent onPublish(JsonMessage<PublishMessageContent> publishMessage) throws F4MException {
		verifyStartupStatus(publishMessage);
		final PublishMessageContent publishContent = publishMessage.getContent();
		LOGGER.debug("onPublish publishContent: {}  publishMessage {} ", publishContent, publishMessage);

		try {
			eventPool.publish(publishContent.getTopic(), publishContent.isVirtual(),
					jsonMessageUtil.toJson(publishMessage), publishContent.getPublishDate());
		} catch (F4MShuttingDownException e) {
			// If shutting down, no need to fail
			LOGGER.debug("Shutdown occured while publishing event message {}, error {}", publishMessage, e);
		}

		return null;
	}

	/**
	 * No authentication need to be performed
	 */
	@Override
	public ClientInfo onAuthentication(RequestContext context) {
		return null;
	}

}
