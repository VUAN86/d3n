package de.ascendro.f4m.service.event;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.event.model.NotifySubscriberMessageContent;
import de.ascendro.f4m.service.event.model.info.InfoRequest;
import de.ascendro.f4m.service.event.model.info.InfoResponse;
import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;
import de.ascendro.f4m.service.event.model.resubscribe.ResubscribeRequest;
import de.ascendro.f4m.service.event.model.resubscribe.ResubscribeResponse;
import de.ascendro.f4m.service.event.model.subscribe.SubscribeRequest;
import de.ascendro.f4m.service.event.model.subscribe.SubscribeResponse;
import de.ascendro.f4m.service.event.model.unsubscribe.UnsubscribeRequestResponse;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;

public class EventMessageTypeMapper extends JsonMessageTypeMapImpl {
	private static final long serialVersionUID = -4575812985735172610L;

	public EventMessageTypeMapper() {
		init();
	}

	protected void init() {
		//Subscribe
		this.register(EventMessageTypes.SUBSCRIBE, new TypeToken<SubscribeRequest>() {
		}.getType());
		this.register(EventMessageTypes.SUBSCRIBE_RESPONSE, new TypeToken<SubscribeResponse>() {
		}.getType());

		//Unsubscribe
		this.register(EventMessageTypes.UNSUBSCRIBE, new TypeToken<UnsubscribeRequestResponse>() {
		}.getType());

		//Resubscribe
		this.register(EventMessageTypes.RESUBSCRIBE, new TypeToken<ResubscribeRequest>() {
		}.getType());
		this.register(EventMessageTypes.RESUBSCRIBE_RESPONSE, new TypeToken<ResubscribeResponse>() {
		}.getType());

		//Info
		this.register(EventMessageTypes.INFO, new TypeToken<InfoRequest>() {
		}.getType());
		this.register(EventMessageTypes.INFO_RESPONSE, new TypeToken<InfoResponse>() {
		}.getType());

		//Publish-Notify
		this.register(EventMessageTypes.PUBLISH, new TypeToken<PublishMessageContent>() {
		}.getType());
		this.register(EventMessageTypes.NOTIFY_SUBSCRIBER, new TypeToken<NotifySubscriberMessageContent>() {
		}.getType());
	}
}
