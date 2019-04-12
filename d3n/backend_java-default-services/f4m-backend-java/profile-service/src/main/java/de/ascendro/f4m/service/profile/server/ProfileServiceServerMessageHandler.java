package de.ascendro.f4m.service.profile.server;

import com.aerospike.client.AerospikeException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.ascendro.f4m.server.exception.F4MAerospikeException;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.service.auth.model.register.SetUserRoleRequest;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.json.model.user.UserRole;
import de.ascendro.f4m.service.payment.model.config.InsertOrUpdateUserRequest;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.profile.client.PaymentUserRequestInfo;
import de.ascendro.f4m.service.profile.dao.EndConsumerInvoiceAerospikeDao;
import de.ascendro.f4m.service.profile.model.*;
import de.ascendro.f4m.service.profile.model.api.get.ProfileGetResponse;
import de.ascendro.f4m.service.profile.model.api.update.ProfileUpdateRequest;
import de.ascendro.f4m.service.profile.model.api.update.ProfileUpdateResponse;
import de.ascendro.f4m.service.profile.model.create.CreateProfileRequest;
import de.ascendro.f4m.service.profile.model.create.CreateProfileResponse;
import de.ascendro.f4m.service.profile.model.delete.DeleteProfileRequest;
import de.ascendro.f4m.service.profile.model.delete.DeleteProfileResponse;
import de.ascendro.f4m.service.profile.model.find.FindListByIdentifiersRequest;
import de.ascendro.f4m.service.profile.model.find.FindListByIdentifiersResponse;
import de.ascendro.f4m.service.profile.model.find.FindProfileRequest;
import de.ascendro.f4m.service.profile.model.find.FindProfileResponse;
import de.ascendro.f4m.service.profile.model.get.app.GetAppConfigurationRequest;
import de.ascendro.f4m.service.profile.model.get.app.GetAppConfigurationResponse;
import de.ascendro.f4m.service.profile.model.get.profile.GetProfileRequest;
import de.ascendro.f4m.service.profile.model.get.profile.GetProfileResponse;
import de.ascendro.f4m.service.profile.model.invoice.EndConsumerInvoiceListRequest;
import de.ascendro.f4m.service.profile.model.invoice.EndConsumerInvoiceListResponse;
import de.ascendro.f4m.service.profile.model.list.ProfileListByIdsRequest;
import de.ascendro.f4m.service.profile.model.list.ProfileListByIdsResponse;
import de.ascendro.f4m.service.profile.model.merge.MergeProfileRequest;
import de.ascendro.f4m.service.profile.model.merge.MergeProfileResponse;
import de.ascendro.f4m.service.profile.model.merge.ProfileMergeEvent;
import de.ascendro.f4m.service.profile.model.resync.ResyncRequest;
import de.ascendro.f4m.service.profile.model.resync.ResyncResponse;
import de.ascendro.f4m.service.profile.model.sub.get.GetProfileBlobRequest;
import de.ascendro.f4m.service.profile.model.sub.get.GetProfileBlobResponse;
import de.ascendro.f4m.service.profile.model.sub.update.UpdateProfileBlobRequest;
import de.ascendro.f4m.service.profile.model.sub.update.UpdateProfileBlobResponse;
import de.ascendro.f4m.service.profile.model.update.UpdateProfileRequest;
import de.ascendro.f4m.service.profile.model.update.UpdateProfileResponse;
import de.ascendro.f4m.service.profile.util.ProfileUtil;
import de.ascendro.f4m.service.profile.util.ProfileUtil.ProfileUpdateInfo;
import de.ascendro.f4m.service.util.auth.UserRoleUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Profile Service Jetty Server websocket message handler
 */
public class ProfileServiceServerMessageHandler extends DefaultJsonMessageHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileServiceServerMessageHandler.class);
	private static final String NO_TENANT_ID = "NO_TENANT";
	private static final String NO_APP_ID = "NO_APP";

	private final ProfileUtil profileUtil;
	private final ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao;
	private final DependencyServicesCommunicator dependencyServicesCommunicator;
	private final EndConsumerInvoiceAerospikeDao endConsumerInvoiceAerospikeDao;
	
	public ProfileServiceServerMessageHandler(ProfileUtil profileUtil,
			ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao,
			DependencyServicesCommunicator dependencyServicesCommunicator,
			EndConsumerInvoiceAerospikeDao endConsumerInvoiceAerospikeDao) {
		this.profileUtil = profileUtil;
		this.applicationConfigurationAerospikeDao = applicationConfigurationAerospikeDao;
		this.dependencyServicesCommunicator = dependencyServicesCommunicator;
		this.endConsumerInvoiceAerospikeDao = endConsumerInvoiceAerospikeDao;
	}

	@Override
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> message)
			throws F4MException {
		final ProfileMessageTypes profileMessageType = message.getType(ProfileMessageTypes.class);
		switch (profileMessageType) {
		case CREATE_PROFILE:
			return onCreateProfile((CreateProfileRequest) message.getContent(), message.getClientInfo());
		case DELETE_PROFILE:
			return onDeleteProfile((DeleteProfileRequest) message.getContent());
		case GET_PROFILE:
			return onGetProfileInternal(message.getClientInfo(), (GetProfileRequest) message.getContent());
		case PROFILE_GET:
			return onProfileGetPublic(message.getClientInfo());
		case UPDATE_PROFILE:
            return onUpdateProfileInternal(message);
		case PROFILE_UPDATE:
            return onProfileUpdatePublic(message.getClientInfo(), message);
		case MERGE_PROFILE:
			return onMergeProfile((MergeProfileRequest) message.getContent());
		case FIND_BY_IDENTIFIER:
			return onFindProfile((FindProfileRequest) message.getContent());
        case FIND_LIST_BY_IDENTIFIERS:
            return onFindProfilesByIdentifiers((FindListByIdentifiersRequest) message.getContent());
		case PROFILE_LIST_BY_IDS:
			return onProfileListByIds((ProfileListByIdsRequest) message.getContent());
		case GET_APP_CONFIGURATION:
			return onGetAppConfiguration(message.getClientInfo(), message);
		case GET_PROFILE_BLOB:
			return onGetProfileBlob(message.getClientInfo(), (GetProfileBlobRequest) message.getContent());
		case UPDATE_PROFILE_BLOB:
			return onUpdateProfileBlob(message.getClientInfo(), (UpdateProfileBlobRequest) message.getContent());
		case RESYNC:
			return onResync((ResyncRequest) message.getContent());
		case END_CONSUMER_INVOICE_LIST:
			return onEndConsumerInvoiceList((EndConsumerInvoiceListRequest) message.getContent(), message.getClientInfo());
		default:
			throw new F4MValidationFailedException("Unsupported message type[" + profileMessageType + "]");
		}
	}

    private EndConsumerInvoiceListResponse onEndConsumerInvoiceList(EndConsumerInvoiceListRequest request, ClientInfo clientInfo) {
    	request.validateFilterCriteria(EndConsumerInvoiceListRequest.MAX_LIST_LIMIT);
		EndConsumerInvoiceListResponse response;

		if (request.getLimit() == 0) {
			// empty response
			response = new EndConsumerInvoiceListResponse(request.getLimit(), request.getOffset());
		} else {
			List<JsonObject> items = endConsumerInvoiceAerospikeDao.getInvoiceList(clientInfo.getTenantId(), clientInfo.getUserId(),
					request.getOffset(), request.getLimit());
			response = new EndConsumerInvoiceListResponse(request.getLimit(), request.getOffset(), items.size(),
					items);
		}
		return response;
	}

	private JsonMessageContent onFindProfilesByIdentifiers(FindListByIdentifiersRequest findListByIdentifiersRequest) {
        List<JsonObject> profiles = new ArrayList<>();
        if (findListByIdentifiersRequest != null) {
            profiles = profileUtil.findListByIdentifiers(findListByIdentifiersRequest.getIdentifiers());
        }
        return new FindListByIdentifiersResponse(profiles);
    }

	protected GetProfileBlobResponse onGetProfileBlob(ClientInfo clientInfo, GetProfileBlobRequest getProfileBlobRequest)
			throws F4MInsufficientRightsException, F4MValidationFailedException {
		if (getProfileBlobRequest != null) {
			GetProfileBlobResponse result;

			final String userId = getUserId(clientInfo, getProfileBlobRequest);

			if (!StringUtils.isBlank(userId)) {
				final JsonElement blobJsonElement = profileUtil.getProfileBlob(userId, getProfileBlobRequest.getName());
				result = new GetProfileBlobResponse(getProfileBlobRequest.getName(), blobJsonElement);
			} else {
				throw new F4MInsufficientRightsException("No user id specified");
			}
			return result;
		} else {
			throw new F4MValidationFailedException("Missing content");
		}
	}

	protected UpdateProfileBlobResponse onUpdateProfileBlob(ClientInfo clientInfo,
			UpdateProfileBlobRequest updateProfileRequest)
					throws F4MInsufficientRightsException, F4MValidationFailedException {
		if (updateProfileRequest != null && updateProfileRequest.getValue() != null) {
			UpdateProfileBlobResponse result;

			final String userId = getUserId(clientInfo, updateProfileRequest);

			if (!StringUtils.isBlank(userId)) {
				final JsonElement profileBlobValue = updateProfileRequest.getValue();
				final JsonElement blobJsonElement = profileUtil.updateProfileBlob(userId,
						updateProfileRequest.getName(), profileBlobValue.toString());
				result = new UpdateProfileBlobResponse(updateProfileRequest.getName(), blobJsonElement);
			} else {
				throw new F4MInsufficientRightsException("No user id specified");
			}

			return result;
		} else {
			throw new F4MValidationFailedException("Missing content");
		}
	}

	@SuppressWarnings("unchecked")
	protected CreateProfileResponse onCreateProfile(CreateProfileRequest createProfileRequest, ClientInfo clientInfo) throws F4MException {
		final ProfileIdentifierType[] profileIdTypes = createProfileRequest.getTypes();
		
		final Pair<ProfileIdentifierType, String>[] profileIdentifiers;
		if (profileIdTypes != null) {
			profileIdentifiers = Arrays.stream(profileIdTypes)
					.map(t -> new ImmutablePair<ProfileIdentifierType, String>(t, createProfileRequest.getValue(t)))
					.toArray(size -> new Pair[size]);
		} else {
			profileIdentifiers = null;
		}
		
		String countryCode = null;
		if (clientInfo != null) {
			countryCode = clientInfo.getCountryCodeAsString();
		}
		final String userId = profileUtil.createProfile(countryCode, profileIdentifiers);
		return new CreateProfileResponse(userId);
	}

	protected DeleteProfileResponse onDeleteProfile(DeleteProfileRequest deleteProfileRequest) throws F4MFatalErrorException {
		String userId = deleteProfileRequest == null ? null : deleteProfileRequest.getUserId();
		if (userId != null) {
			try {
				profileUtil.deleteProfile(userId);
			} catch (F4MIOException e) {
				LOGGER.warn("Cannot publish " + ProfileUtil.PROFILE_DELETE_EVENT_TOPIC + " event: " + e.getMessage(), e);
			}
			return new DeleteProfileResponse();
		} else {
			throw new F4MInsufficientRightsException("User must be specified");
		}
	}

	protected UpdateProfileResponse onUpdateProfileInternal(JsonMessage<? extends JsonMessageContent> message)
			throws F4MException {
		final ProfileUpdateInfo resultProfile;
		final UpdateProfileRequest updateProfileRequest = (UpdateProfileRequest) message.getContent();
        String userId;
		final Profile profile;
		Pair<ProfileUpdateInfo, Profile> updateResult;

		if (updateProfileRequest.getProfile() != null) {
			profile = new Profile(updateProfileRequest.getProfile());

			if (updateProfileRequest.getUserId() != null) {
				userId = updateProfileRequest.getUserId();
			} else {
				throw new F4MInsufficientRightsException("Mandatory fields missing");
			}
            LOGGER.debug("  onUpdateProfileInternal   1");
            updateResult = profileUtil.updateProfile(userId, profile, true, this.isMessageFromClient(message), true, updateProfileRequest.isServiceResultEngineOrGamEngine());
			LOGGER.debug("onUpdateProfileInternal  updateResult  [ {} ]   [ {} ]   [ {} ]   [ {} ] ",
                    updateResult.getKey().getProfile(), updateResult.getLeft().getProfile().getHandicap(), updateResult.getRight().getHandicap(), updateResult.getValue().getHandicap());
			resultProfile = updateResult.getLeft();
		} else {
			throw new F4MValidationFailedException("Mandatory fields missing");
		}
		if (resultProfile.isRecommendedByAdded()) {
            dependencyServicesCommunicator.sendBuddyAddForUser(resultProfile.getProfile().getUserId(),
					resultProfile.getProfile().getRecommendedBy());
			dependencyServicesCommunicator.sendBuddyAddForUser(resultProfile.getProfile().getRecommendedBy(),
					resultProfile.getProfile().getUserId());
		}
		synchronizeUpdatedData(resultProfile, updateResult.getRight(), userId, message, NO_TENANT_ID, NO_APP_ID);
		return new UpdateProfileResponse(resultProfile.getProfile().getJsonObject());
	}

	private void synchronizeUpdatedData(ProfileUpdateInfo resultProfile, Profile originalProfile, String userId,
			JsonMessage<?> sourceMessage, String tenantId, String appId) {
		updateUserRole(userId, tenantId, appId, resultProfile.getProfile());
		if (ProfileUpdateValidator.isProfileUpdateRelevantForPayment(originalProfile, resultProfile.getProfile())) {
			synchronizeProfileToPaymentServiceReturnIfMustWait(resultProfile.getProfile(), Collections.emptyList(), sourceMessage, null);
		}
	}

	private void updateUserRole(String userId, String tenantId, String appId, Profile profile) {
		if (isFullyRegisteredRole(profile)) {
			SetUserRoleRequest setUserRoleRequest = UserRoleUtil.createSetUserRoleRequest(userId,
					profile.getRoles(tenantId), UserRole.FULLY_REGISTERED, UserRole.ANONYMOUS);
			if (setUserRoleRequest != null) {
				dependencyServicesCommunicator.updateUserRoles(setUserRoleRequest, appId, tenantId);
			}
		}
	}

	protected boolean isFullyRegisteredRole(Profile profile) {
		boolean result = false;
		if (profile != null && profile.getPersonWrapper() != null) {
			ProfileUser person = profile.getPersonWrapper();
			boolean hasAddress = StringUtils.isNoneEmpty(profile.getCountry(), profile.getCity());
			boolean hasPersonName = StringUtils.isNoneEmpty(person.getFirstName(), person.getLastName(),
					person.getNickname());
			result = hasAddress && hasPersonName;
		}
		return result;
	}

	private boolean synchronizeProfileToPaymentServiceReturnIfMustWait(Profile profile, List<String> newTenantIds,
			JsonMessage<?> sourceMessage, GetAppConfigurationResponse response) {
		AtomicInteger numberOfResponsesToWait = new AtomicInteger();
		boolean mustWaitForResponseFromPayment = false;
		if (profile.getTenants() != null && profile.getUserId() != null) {
			for (String tenantId : profile.getTenants()) {
				InsertOrUpdateUserRequest synchronizeRequest = new InsertOrUpdateUserRequest();
				synchronizeRequest.setTenantId(tenantId);
				synchronizeRequest.setProfileId(profile.getUserId());
				synchronizeRequest.setProfile(profile.getJsonObject());
				boolean newUserInPaymentSystem = newTenantIds.contains(tenantId);
				synchronizeRequest.setInsertNewUser(newUserInPaymentSystem);
				PaymentUserRequestInfo requestInfo = new PaymentUserRequestInfo(sourceMessage, this.getSessionWrapper());
				if (newUserInPaymentSystem) {
					//will wait only for creation of new users, since name changes do not affect account existence and balances
					numberOfResponsesToWait.incrementAndGet();
					requestInfo.setNumberOfExpectedResponses(numberOfResponsesToWait);
					requestInfo.setResponseToForward(response);
					requestInfo.setSourceSession(this.getSessionWrapper());
					mustWaitForResponseFromPayment = true;
				}
				dependencyServicesCommunicator.synchronizeProfileToPaymentService(synchronizeRequest, requestInfo);
			}
		}
		return mustWaitForResponseFromPayment;
	}
	
	protected ProfileUpdateResponse onProfileUpdatePublic(ClientInfo clientInfo, JsonMessage<? extends JsonMessageContent> message)
			throws F4MException {
		final ProfileUpdateInfo resultProfile;
		final ProfileUpdateRequest updateProfileRequest = (ProfileUpdateRequest) message.getContent();
		Validate.notNull(updateProfileRequest);

		String userId;
		if (clientInfo != null && !StringUtils.isBlank(clientInfo.getUserId())) {
			userId = clientInfo.getUserId();
		} else {
			throw new F4MInsufficientRightsException("No user information");
		}
		final Profile profile = updateProfileRequest.getProfile().toProfile();
        Pair<ProfileUpdateInfo, Profile> updateResult = profileUtil.updateProfile(userId, profile, true, false, false, false);
		resultProfile = updateResult.getLeft(); 
		synchronizeUpdatedData(resultProfile, updateResult.getRight(), userId, message, clientInfo.getTenantId(), clientInfo.getAppId());
		return new ProfileUpdateResponse(resultProfile.getProfile());
	}

	protected MergeProfileResponse onMergeProfile(MergeProfileRequest mergeProfileRequest) throws F4MException {
		if (mergeProfileRequest != null && mergeProfileRequest.getSource() != null
				&& mergeProfileRequest.getTarget() != null) {
			final Profile sourceProfile = profileUtil.getActiveProfile(mergeProfileRequest.getSource());
			final Profile targetProfile = profileUtil.getActiveProfile(mergeProfileRequest.getTarget());
			if (sourceProfile != null && targetProfile != null) {
				Profile resultProfile = null;
				try {
					resultProfile = profileUtil.mergeProfiles(sourceProfile, targetProfile);
				} catch (F4MIOException e) {
					LOGGER.warn("Cannot publish " + ProfileUtil.PROFILE_DELETE_EVENT_TOPIC
							+ " event on merged source profile delete: " + e.getMessage(), e);
				}
				try {
					profileUtil.publishProfileMergeEvent(mergeProfileRequest);
				} catch (F4MIOException e) {
					LOGGER.error("Cannot publish " + ProfileMergeEvent.PROFILE_MERGE_EVENT_TOPIC
							+ " event on profile merge: " + e.getMessage(), e);
				}
				return resultProfile == null ? null : new MergeProfileResponse(resultProfile.getJsonObject());
			} else {
				final String errorMessage;
				if (sourceProfile == null) {
					errorMessage = "Source profile[" + mergeProfileRequest.getSource()
							+ "] requested for merge not found";
				} else {
					errorMessage = "Target profile[" + mergeProfileRequest.getTarget()
							+ "] requested for merge not found";
				}

				throw new F4MFatalErrorException(errorMessage);
			}
		} else {
			throw new F4MValidationFailedException("Mandatory fields missing");
		}
	}

	protected FindProfileResponse onFindProfile(FindProfileRequest findProfileRequest) throws F4MException {
		if (findProfileRequest != null && findProfileRequest.getIdentifierType() != null
				&& findProfileRequest.getIdentifier() != null) {
			final String userId = profileUtil.findByIdentifier(findProfileRequest.getIdentifierType(),
					findProfileRequest.getIdentifier());
			return new FindProfileResponse(userId);
		} else {
			throw new F4MValidationFailedException("Mandatory fields missing");
		}
	}

	protected ProfileListByIdsResponse onProfileListByIds(ProfileListByIdsRequest profileListByIdsRequest)
			throws F4MException {
		List<JsonObject> profiles = new ArrayList<>();
		if (profileListByIdsRequest != null) {
			profiles = profileUtil.getProfileListByIds(profileListByIdsRequest.getProfilesIds());
		}
		return new ProfileListByIdsResponse(profiles);
	}

	protected GetProfileResponse onGetProfileInternal(ClientInfo clientInfo, GetProfileRequest getProfileRequest)
			throws F4MValidationFailedException {
		String userId;
		LOGGER.debug("onGetProfileInternal clientInfo={}",clientInfo);
		if (clientInfo != null && clientInfo.getUserId() != null) {
			userId = clientInfo.getUserId();
		} else if (getProfileRequest != null && getProfileRequest.getUserId() != null) {
			userId = getProfileRequest.getUserId();
		} else {
			throw new F4MValidationFailedException("Mandatory fields missing");
		}
		LOGGER.debug("onGetProfileInternal");
		GetProfileResponse response = new GetProfileResponse();

		final Profile resultProfile;
		LOGGER.debug("onGetProfileInternal");
		if (getProfileRequest.getReturnMergedUser() != null && getProfileRequest.getReturnMergedUser()) {
			resultProfile = profileUtil.getProfile(userId);
		} else {
			resultProfile = profileUtil.getActiveProfile(userId);
		}
		LOGGER.debug("onGetProfileInternal");
		if (resultProfile != null) {
			response.setProfile(resultProfile.getJsonObject());
			LOGGER.debug("onGetProfileInternal response {} ", response);
			return response;
		} else {
			throw new F4MEntryNotFoundException();
		}
	}

	protected ProfileGetResponse onProfileGetPublic(ClientInfo clientInfo) throws F4MValidationFailedException {
		LOGGER.debug("onProfileGetPublic clientInfo={} message={}",clientInfo);
		String userId;
		String tenantId;
		if (clientInfo != null && clientInfo.getUserId() != null) {
			userId = clientInfo.getUserId();
			if (clientInfo.getTenantId() != null) {
				tenantId = clientInfo.getTenantId();
			} else {
				throw new F4MInsufficientRightsException("No tenant information");
			}
		} else {
			throw new F4MInsufficientRightsException("No user information");
		}
		final Profile profile = profileUtil.getActiveProfile(userId);
		final ProfileStats profileStats = profileUtil.getProfileStats(userId, tenantId);
		return new ProfileGetResponse(profile, profileStats);
	}

	private JsonMessageContent onResync(ResyncRequest resyncRequest) {
		int count = profileUtil.queueResync(resyncRequest.getUserId());
		return new ResyncResponse(count);
	}

	protected GetAppConfigurationResponse onGetAppConfiguration(ClientInfo clientInfo,
			JsonMessage<? extends JsonMessageContent> message) throws F4MException {
		LOGGER.debug("onGetAppConfiguration clientInfo={} message={}",clientInfo,message);
		GetAppConfigurationResponse getAppConfigurationResponse = null;
		GetAppConfigurationRequest getAppConfigurationRequest = (GetAppConfigurationRequest) message.getContent();
		if (getAppConfigurationRequest != null) {
			GetAppConfigurationResponse response = new GetAppConfigurationResponse(
					applicationConfigurationAerospikeDao.getAppConfigurationAsJsonElement(
							getAppConfigurationRequest.getTenantId(), getAppConfigurationRequest.getAppId()));
			boolean mustWaitForResponseFromPayment = false;
			List<String> newTenantIds = updateProfileStatistics(clientInfo, getAppConfigurationRequest);
			if (!newTenantIds.isEmpty()) { //update only, if new user to the tenant
				Profile profile = profileUtil.getActiveProfile(clientInfo.getUserId());
				//waiting for answer from payments necessary only here, not from profileUpdate
				mustWaitForResponseFromPayment = synchronizeProfileToPaymentServiceReturnIfMustWait(profile,
						newTenantIds, message, response);
			}
			if (!mustWaitForResponseFromPayment) { //do not respond immediately, but wait for all responses from payment
				getAppConfigurationResponse = response;
			}
			LOGGER.debug("onGetAppConfiguration finished with response {}, will wait for {} responses from payment",
					getAppConfigurationResponse, mustWaitForResponseFromPayment);
		} else {
			throw new F4MValidationFailedException("Content is mandatory");
		}

		return getAppConfigurationResponse;
	}

	private List<String> updateProfileStatistics(ClientInfo clientInfo, GetAppConfigurationRequest getAppConfigurationRequest)
			throws F4MException {
		if (clientInfo != null && !StringUtils.isBlank(clientInfo.getUserId())) {



			return profileUtil.updateProfileStatistics(clientInfo.getUserId(), getAppConfigurationRequest.getDeviceUUID(),
					getAppConfigurationRequest.getDevice(), getAppConfigurationRequest.getAppId(),
					getAppConfigurationRequest.getTenantId(), getAppConfigurationRequest.getIMEI(), getAppConfigurationRequest.getOneSignalDeviceId());
		} else {
			throw new F4MInsufficientRightsException(
					"No user authenticated for " + ProfileMessageTypes.GET_APP_CONFIGURATION.getShortName() + " call");
		}
	}

	@Override
	protected void sendErrorMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded, Throwable cause) {
		final Throwable resultException;
		if (cause instanceof AerospikeException) {
			resultException = new F4MAerospikeException((AerospikeException) cause);
		} else {
			resultException = cause;
		}
		super.sendErrorMessage(originalMessageDecoded, resultException);
	}

}
