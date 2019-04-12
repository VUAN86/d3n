package de.ascendro.f4m.service.tombola.server;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import de.ascendro.f4m.service.json.handler.JsonAuthenticationMessageMQHandler;
import de.ascendro.f4m.service.tombola.exception.TombolaNoTicketsAvailableException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.tombola.TombolaDrawingEngine;
import de.ascendro.f4m.service.tombola.TombolaManager;
import de.ascendro.f4m.service.tombola.TombolaMessageTypes;
import de.ascendro.f4m.service.tombola.dao.TombolaAerospikeDao;
import de.ascendro.f4m.service.tombola.exception.TombolaNoLongerAvailableException;
import de.ascendro.f4m.service.tombola.model.Bundle;
import de.ascendro.f4m.service.tombola.model.MoveTombolasRequest;
import de.ascendro.f4m.service.tombola.model.Tombola;
import de.ascendro.f4m.service.tombola.model.TombolaDrawing;
import de.ascendro.f4m.service.tombola.model.TombolaStatus;
import de.ascendro.f4m.service.tombola.model.UserTombolaInfo;
import de.ascendro.f4m.service.tombola.model.buy.TombolaBuyRequest;
import de.ascendro.f4m.service.tombola.model.drawing.TombolaDrawingGetRequest;
import de.ascendro.f4m.service.tombola.model.drawing.TombolaDrawingGetResponse;
import de.ascendro.f4m.service.tombola.model.drawing.TombolaDrawingListRequest;
import de.ascendro.f4m.service.tombola.model.drawing.TombolaDrawingListResponse;
import de.ascendro.f4m.service.tombola.model.events.TombolaEventRequest;
import de.ascendro.f4m.service.tombola.model.get.TombolaBuyerListRequest;
import de.ascendro.f4m.service.tombola.model.get.TombolaBuyerListResponse;
import de.ascendro.f4m.service.tombola.model.get.TombolaGetRequest;
import de.ascendro.f4m.service.tombola.model.get.TombolaGetResponse;
import de.ascendro.f4m.service.tombola.model.get.TombolaListRequest;
import de.ascendro.f4m.service.tombola.model.get.TombolaListResponse;
import de.ascendro.f4m.service.tombola.model.get.TombolaWinnerListRequest;
import de.ascendro.f4m.service.tombola.model.get.TombolaWinnerListResponse;
import de.ascendro.f4m.service.tombola.model.get.UserTombolaGetRequest;
import de.ascendro.f4m.service.tombola.model.get.UserTombolaGetResponse;
import de.ascendro.f4m.service.tombola.model.get.UserTombolaInfoResponseModel;
import de.ascendro.f4m.service.tombola.model.get.UserTombolaListRequest;
import de.ascendro.f4m.service.tombola.model.get.UserTombolaListResponse;
import de.ascendro.f4m.service.util.DateTimeUtil;


public class TombolaServiceServerMessageHandler extends JsonAuthenticationMessageMQHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TombolaAerospikeDao.class);

    private final TombolaManager tombolaManager;
    private final TombolaDrawingEngine tombolaDrawingEngine;

    public TombolaServiceServerMessageHandler(TombolaManager tombolaManager, TombolaDrawingEngine tombolaDrawingEngine) {
        this.tombolaManager = tombolaManager;
        this.tombolaDrawingEngine = tombolaDrawingEngine;
    }

    @Override
    public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> message) {
		final TombolaMessageTypes tombolaMessageType;

		if ((tombolaMessageType = message.getType(TombolaMessageTypes.class)) != null) {
			return onTombolaMessage(message.getClientInfo(), message, tombolaMessageType);
		} else {
			throw new F4MValidationFailedException("Unrecognized message type");
		}		
    }
    
	private JsonMessageContent onTombolaMessage(ClientInfo clientInfo,
			JsonMessage<? extends JsonMessageContent> message, TombolaMessageTypes tombolaMessageType) {
        switch (tombolaMessageType) {
        case TOMBOLA_GET:
            return onGetTombola((TombolaGetRequest) message.getContent());
        case TOMBOLA_LIST:
            return onGetTombolaListByAppId(clientInfo, (TombolaListRequest) message.getContent());
        case TOMBOLA_BUY:
            return onTombolaBuy(clientInfo, message);
        case TOMBOLA_DRAWING_GET:
            return onTombolaDrawingGet((TombolaDrawingGetRequest) message.getContent());
        case TOMBOLA_DRAWING_LIST:
            return onTombolaDrawingList(clientInfo, (TombolaDrawingListRequest) message.getContent());
        case USER_TOMBOLA_LIST:
            return onUserTombolaList(clientInfo, (UserTombolaListRequest) message.getContent());
        case USER_TOMBOLA_GET:
            return onUserTombolaGet(clientInfo, (UserTombolaGetRequest) message.getContent());
        case MOVE_TOMBOLAS:
        	return onMoveTombolas((MoveTombolasRequest) message.getContent());
		case TOMBOLA_WINNER_LIST:
			return onTombolaWinnerList((TombolaWinnerListRequest) message.getContent());
        case TOMBOLA_BUYER_LIST:
            return onTombolaBuyerList((TombolaBuyerListRequest) message.getContent());
        case TOMBOLA_OPEN_CHECKOUT:
            return onOpenCheckout((TombolaEventRequest) message.getContent());
        case TOMBOLA_CLOSE_CHECKOUT:
            return onCloseCheckout((TombolaEventRequest) message.getContent());
        case TOMBOLA_DRAW:
            return onTombolaDraw(message, message.getClientInfo(), (TombolaEventRequest) message.getContent());
        default:
            throw new F4MValidationFailedException("Unsupported message type[" + tombolaMessageType + "]");
        }
    }

	private TombolaDrawingGetResponse onTombolaDrawingGet(TombolaDrawingGetRequest request) {
        TombolaDrawing drawing = tombolaManager.getTombolaDrawing(request.getTombolaId());
        return new TombolaDrawingGetResponse(drawing);
    }

    private TombolaGetResponse onGetTombola(TombolaGetRequest request) {
        Tombola tombola = tombolaManager.getTombola(request.getTombolaId());
        return new TombolaGetResponse(tombola);
    }

    private TombolaDrawingListResponse onTombolaDrawingList(ClientInfo clientInfo,
            TombolaDrawingListRequest tombolaDrawingListRequest) {
        tombolaDrawingListRequest.validateFilterCriteria(TombolaListRequest.MAX_LIST_LIMIT);

        ZonedDateTime dateFrom = DateTimeUtil.parseISODateTimeString(tombolaDrawingListRequest.getDateFrom());
        ZonedDateTime dateTo = DateTimeUtil.parseISODateTimeString(tombolaDrawingListRequest.getDateTo());

        if (dateTo.isBefore(dateFrom)) {
            throw new F4MValidationFailedException("dateFrom must be before dateTo");
        }

        final String appId = clientInfo.getAppId();

        if (StringUtils.isBlank(appId)) {
            throw new F4MInsufficientRightsException("No app id specified");
        }

        LOGGER.debug("Getting tombolas for app <{}>", appId);

        TombolaDrawingListResponse result;

        if (tombolaDrawingListRequest.getLimit() == 0) {
            // empty response
            result = new TombolaDrawingListResponse(tombolaDrawingListRequest.getLimit(),
                    tombolaDrawingListRequest.getOffset());
        } else {
            result = tombolaManager.getTombolaDrawingListResponse(appId, tombolaDrawingListRequest.getOffset(),
                    tombolaDrawingListRequest.getLimit(), dateFrom, dateTo);
        }

        return result;
    }

    private UserTombolaListResponse onUserTombolaList(ClientInfo clientInfo,
            UserTombolaListRequest userTombolaListRequest) {
        userTombolaListRequest.validateFilterCriteria(TombolaListRequest.MAX_LIST_LIMIT);

        if (StringUtils.isBlank(clientInfo.getUserId())) {
            throw new F4MInsufficientRightsException("No user id specified");
        }

        ZonedDateTime dateFrom = DateTimeUtil.parseISODateTimeString(userTombolaListRequest.getDateFrom());
        ZonedDateTime dateTo = DateTimeUtil.parseISODateTimeString(userTombolaListRequest.getDateTo());

        if (dateTo.isBefore(dateFrom)) {
            throw new F4MValidationFailedException("dateFrom must be before dateTo");
        }

        LOGGER.debug("Getting tombola history for user <{}>", clientInfo.getUserId());

        UserTombolaListResponse result;

        if (userTombolaListRequest.getLimit() == 0) {
            // empty response
            result = new UserTombolaListResponse(userTombolaListRequest.getLimit(),
                    userTombolaListRequest.getOffset());
        } else {
            result = tombolaManager.getUserTombolaListResponse(clientInfo.getUserId(),
                    userTombolaListRequest.getOffset(), userTombolaListRequest.getLimit(), dateFrom, dateTo,
                    userTombolaListRequest.getPendingDrawing());
        }

        return result;
    }

    private UserTombolaGetResponse onUserTombolaGet(ClientInfo clientInfo, UserTombolaGetRequest request) {
        UserTombolaInfo userTombolaInfo = tombolaManager.getUserTombola(clientInfo.getUserId(), request.getTombolaId());

        return new UserTombolaGetResponse(new UserTombolaInfoResponseModel(userTombolaInfo));
    }

    private TombolaListResponse onGetTombolaListByAppId(ClientInfo clientInfo, TombolaListRequest request) {
        request.validateFilterCriteria(TombolaListRequest.MAX_LIST_LIMIT);


        if (request.getLimit() == 0) {
            return new TombolaListResponse(request.getLimit(), request.getOffset()); // empty response
        }

        final String appId = clientInfo.getAppId();

        LOGGER.debug("Getting tombolas for app <{}>", appId);

        return tombolaManager.getTombolaListResponseByAppId(request, appId);
    }

    private JsonMessageContent onTombolaBuy(ClientInfo clientInfo, JsonMessage<? extends JsonMessageContent> message) {
        TombolaBuyRequest request = (TombolaBuyRequest) message.getContent();
        Tombola tombola = tombolaManager.getTombola(request.getTombolaId());

        final String appId = clientInfo.getAppId();
        final String tenantId = clientInfo.getTenantId();

        boolean isAnonymous = false;
        if (clientInfo !=null){
            Set<String> roles = new HashSet<String>(Arrays.asList(clientInfo.getRoles()));
            isAnonymous = roles.contains("ANONYMOUS");
        }

        if (!tombola.isInfinityTickets() && (tombola.getPurchasedTicketsAmount()+ request.getTickets()) > tombola.getTotalTicketsAmount()) {
            throw  new TombolaNoTicketsAvailableException("Tombola does not have "+request.getTickets()+" tickets.");
        }

        if (isAnonymous) {
            throw  new TombolaNoLongerAvailableException("Tombola cannot be bought by Anonymous.");
        }

        if (tombola.getStatus() == null) {
            throw  new TombolaNoLongerAvailableException("Tombola status is invalid.");
        }

        if(!tombola.getStatus().equals(TombolaStatus.ACTIVE)) {
            throw  new TombolaNoLongerAvailableException("Tickets can no longer be purchased for the tombola.");
        }

        if(tombola.getApplicationsIds() == null || !tombola.getApplicationsIds().contains(appId)) {
            throw new F4MEntryNotFoundException("Tombola is not available for this application");
        }

        Bundle bundle = tombolaManager.getBundleToPurchase(tombola, request.getTickets());
        if(bundle == null) {
            throw new F4MEntryNotFoundException("Bundle not found");
        }

        // lock the tickets :
        tombolaManager.lockTickets(bundle, clientInfo.getUserId(), tombola);

        // initiate the payment :
        tombolaManager.initiateBuyTicketsPaymentTransfer(message, bundle, clientInfo.getUserId(),
                tombola.getId(), appId, tenantId);
        // response is sent from ClientMessageHandler
        
        return null;
    }

    private JsonMessageContent onMoveTombolas(MoveTombolasRequest request) {
		tombolaManager.moveTombolas(request.getSourceUserId(), request.getTargetUserId());
		return new EmptyJsonMessageContent();
	}

	private JsonMessageContent onTombolaWinnerList(TombolaWinnerListRequest request) {

        TombolaDrawing tombolaDrawing = tombolaManager.getTombolaWinnerList(request);

        return new TombolaWinnerListResponse(tombolaDrawing);
    }

	private TombolaBuyerListResponse onTombolaBuyerList(TombolaBuyerListRequest request) {
        request.validateFilterCriteria(TombolaBuyerListRequest.MAX_LIST_LIMIT);

        TombolaBuyerListResponse response;
        if (request.getLimit() == 0) {
            response = new TombolaBuyerListResponse(request.getLimit(), request.getOffset()); // empty response
        } else {
            response = tombolaManager.getTombolaBuyerListResponse(request);
        }

        return response;
    }

    private JsonMessageContent onOpenCheckout(TombolaEventRequest request) {
        Tombola tombola = tombolaManager.getTombola(request.getTombolaId());

        ZonedDateTime endDate = DateTimeUtil.parseISODateTimeString(tombola.getEndDate());
        if (!endDate.isBefore(ZonedDateTime.now())) {
            tombolaManager.handleOpenCheckoutEvent(request.getTombolaId());
            tombolaManager.sendPushNotificationOnOpenTombola(tombola);
        } else {
            LOGGER.warn("Stoped OpenCheckout expired Tombola id={}, endDate={}",tombola.getId(),tombola.getEndDate());
        }
        return new EmptyJsonMessageContent();
    }

    private JsonMessageContent onCloseCheckout(TombolaEventRequest request) {
        tombolaManager.handleCloseCheckoutEvent(request.getTombolaId());
        return new EmptyJsonMessageContent();
    }

    private JsonMessageContent onTombolaDraw(JsonMessage<? extends JsonMessageContent> message, ClientInfo clientInfo,
                                             TombolaEventRequest request) {
        tombolaDrawingEngine.tombolaDraw(request.getTombolaId(), clientInfo.getTenantId(), message);
        return new EmptyJsonMessageContent();
    }
}
