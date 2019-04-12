package de.ascendro.f4m.service.advertisement.server;

import de.ascendro.f4m.advertisement.AdvertisementMessageTypes;
import de.ascendro.f4m.advertisement.model.AdvertisementHasBeenShownRequest;
import de.ascendro.f4m.advertisement.model.GameAdvertisementsGetRequest;
import de.ascendro.f4m.advertisement.model.GameAdvertisementsGetResponse;
import de.ascendro.f4m.server.advertisement.AdvertisementManager;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.model.AppConfig;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;


public class AdvertisementServiceServerMessageHandler extends DefaultJsonMessageHandler {

    private AdvertisementManager advertisementManager;
    private ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao;
    public static final int DEFAULT_NUMBER_OF_ADVERTISEMENTS = 5;

    public AdvertisementServiceServerMessageHandler(AdvertisementManager advertisementManager, ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao) {
        this.advertisementManager = advertisementManager;
        this.applicationConfigurationAerospikeDao = applicationConfigurationAerospikeDao;
    }

    @Override
    public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> message) {
        final AdvertisementMessageTypes advertisementMessageType;
        if ((advertisementMessageType = message.getType(AdvertisementMessageTypes.class)) != null) {
            return onAdvertisementMessage(message.getClientInfo(), message, advertisementMessageType);
        } else {
            throw new F4MValidationFailedException("Unrecognized message type " + message.getName());
        }
    }

    private JsonMessageContent onAdvertisementMessage(ClientInfo clientInfo, JsonMessage<? extends JsonMessageContent> message,
                                                      AdvertisementMessageTypes advertisementMessageType) {

        switch (advertisementMessageType) {
            case GAME_ADVERTISEMENTS_GET:
                return onGameAdvertisementsGet(message.getClientInfo(), (GameAdvertisementsGetRequest) message.getContent());

            case ADVERTISEMENT_HAS_BEEN_SHOWN:

                return onAdvertisementHasBeenShown(clientInfo, (AdvertisementHasBeenShownRequest) message.getContent());

            default:
                throw new F4MValidationFailedException("Unsupported message type[" + advertisementMessageType + "]");
        }
    }

	private JsonMessageContent onGameAdvertisementsGet(ClientInfo clientInfo, GameAdvertisementsGetRequest request) {

		AppConfig appConfig = applicationConfigurationAerospikeDao.getAppConfiguration(clientInfo.getTenantId(),
				clientInfo.getAppId());

		int numberOfAdvertisements = DEFAULT_NUMBER_OF_ADVERTISEMENTS;
		if (appConfig != null && isValidNumberOfAdvertisementsAvailable(appConfig)) {
			numberOfAdvertisements = appConfig.getApplication().getConfiguration().getNumberOfPreloadedAdvertisements();
		}

		if (request.getProviderId() == 0) {
			throw new F4MEntryNotFoundException("Invalid advertisementProviderId specified.");
		}

		if (numberOfAdvertisements == 0) {
			numberOfAdvertisements = DEFAULT_NUMBER_OF_ADVERTISEMENTS;
		}

		String[] advertisementBlobs = advertisementManager.getRandomAdvertisementBlobKeys(numberOfAdvertisements,
				request.getProviderId());
		return new GameAdvertisementsGetResponse(advertisementBlobs);

	}

	private boolean isValidNumberOfAdvertisementsAvailable(AppConfig appConfig) {
		boolean validNumberOfAdvertisementsAvailable = false;
		if (appConfig.getApplication() != null
				&& appConfig.getApplication().getConfiguration() != null
				&& appConfig.getApplication().getConfiguration().getNumberOfPreloadedAdvertisements() != 0) {
			validNumberOfAdvertisementsAvailable = true;
		}

		return validNumberOfAdvertisementsAvailable;
	}

    private JsonMessageContent onAdvertisementHasBeenShown(ClientInfo clientInfo, AdvertisementHasBeenShownRequest request) {
        advertisementManager.markAdvertisementShown(clientInfo, request.getAdvertisementBlobKey(), request.getGameId(), request.getGameInstanceId());
        return new EmptyJsonMessageContent();
    }

}
