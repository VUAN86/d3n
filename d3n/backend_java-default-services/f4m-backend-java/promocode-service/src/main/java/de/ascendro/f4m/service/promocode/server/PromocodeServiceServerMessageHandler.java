package de.ascendro.f4m.service.promocode.server;

import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.Currency;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.promocode.PromocodeMessageTypes;
import de.ascendro.f4m.service.promocode.model.UserPromocode;
import de.ascendro.f4m.service.promocode.model.promocode.PromocodeUseRequest;
import de.ascendro.f4m.service.promocode.model.promocode.PromocodeUseResponse;
import de.ascendro.f4m.service.promocode.rest.wrapper.PromoResponseType;
import de.ascendro.f4m.service.promocode.rest.wrapper.PromoRestWrapper;
import de.ascendro.f4m.service.promocode.util.PromocodeManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Promocode Service Jetty Server websocket message handler
 */
public class PromocodeServiceServerMessageHandler extends DefaultJsonMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(PromocodeServiceServerMessageHandler.class);

	private PromocodeManager promocodeManager;
	private PromoRestWrapper promoRestWrapper;

	public PromocodeServiceServerMessageHandler(PromocodeManager promocodeManager) {
	    LOGGER.debug("Create PromocodeServiceServerMessageHandler");
		this.promocodeManager = promocodeManager;
		this.promoRestWrapper = new PromoRestWrapper();
	}

	@Override
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> message) {
		final PromocodeMessageTypes promocodeMessageTypes = message.getType(PromocodeMessageTypes.class);
		JsonMessageContent result;
		switch (promocodeMessageTypes) {
			case PROMOCODE_USE:
        	result = onUserPromocodeUse(message.getClientInfo(), message);
        	break;
		default:
			throw new F4MValidationFailedException("Unsupported message type[" + promocodeMessageTypes + "]");
		}
		return result;
	}

	
	private PromocodeUseResponse onUserExternalPromocodeUse(JSONObject obj){
		String code = obj.getString("code");
		String email = obj.getString("email");
		PromoResponseType promocodeStatus = promoRestWrapper.checkPromoStatus(code);
		if(promocodeStatus == PromoResponseType.VALID){
			Profile profile =  promocodeManager.searchProfilesWithEmail(email);
			if(profile!=null){
				promocodeManager.creditPromocodeAmounts(
					code,
					"10",
					profile.getUserId(),
					2500,
					Currency.BONUS
				);
				return  new PromocodeUseResponse(PromoResponseType.VALID.getValue());		
			} else {
				return  new PromocodeUseResponse(PromoResponseType.INVALID.getValue());		
			}
		}
		return  new PromocodeUseResponse(promocodeStatus.getValue());
	}
	
	private PromocodeUseResponse onUserInternalPromocodeUse(ClientInfo clientInfo, JsonMessage<? extends JsonMessageContent> message){
		if (clientInfo == null || clientInfo.getUserId() == null) {
			throw new F4MInsufficientRightsException("No user id specified");
		}

		final PromocodeUseRequest request = (PromocodeUseRequest) message.getContent();
		final UserPromocode promocode = promocodeManager.usePromocodeForUser(request.getCode(), clientInfo);
		LOGGER.debug("onUserInternalPromocodeUse ConnectedClientServiceName={}",getSessionWrapper().getConnectedClientServiceName());
		promocodeManager.transferPromocodeAmounts(message, getSessionWrapper(), promocode);

		return new PromocodeUseResponse(request.getCode());
	}
	
	private PromocodeUseResponse onUserPromocodeUse(ClientInfo clientInfo, JsonMessage<? extends JsonMessageContent> message){
		final PromocodeUseRequest request = (PromocodeUseRequest) message.getContent();
		String strObj = request.getCode();
		JSONObject obj;
		try {
			obj = new JSONObject(strObj);
		} catch(Exception e) {
			return onUserInternalPromocodeUse(clientInfo, message);
		}
		return onUserExternalPromocodeUse(obj);
	}

}
