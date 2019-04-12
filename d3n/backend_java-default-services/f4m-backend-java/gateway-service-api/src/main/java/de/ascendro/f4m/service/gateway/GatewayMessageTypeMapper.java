package de.ascendro.f4m.service.gateway;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.gateway.model.GatewaySendUserMessageRequest;
import de.ascendro.f4m.service.gateway.model.GatewaySendUserMessageResponse;
import de.ascendro.f4m.service.gateway.model.key.InstanceDisconnectEvent;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;

public class GatewayMessageTypeMapper extends JsonMessageTypeMapImpl {
	private static final long serialVersionUID = -7351971538661104069L;

	public GatewayMessageTypeMapper() {
		init();
	}

	protected void init() {
		this.register(GatewayMessageTypes.CLIENT_DISCONNECT, new TypeToken<EmptyJsonMessageContent>() {});
		this.register(GatewayMessageTypes.INSTANCE_DISCONNECT, new TypeToken<InstanceDisconnectEvent>() {});
		
		this.register(GatewayMessageTypes.SEND_USER_MESSAGE, new TypeToken<GatewaySendUserMessageRequest>() {});
		this.register(GatewayMessageTypes.SEND_USER_MESSAGE_RESPONSE, new TypeToken<GatewaySendUserMessageResponse>() {});
	}
}
