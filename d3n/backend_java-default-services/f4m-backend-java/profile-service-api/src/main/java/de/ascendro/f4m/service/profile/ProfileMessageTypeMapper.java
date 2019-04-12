package de.ascendro.f4m.service.profile;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;
import de.ascendro.f4m.service.profile.model.api.get.ProfileGetRequest;
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
import de.ascendro.f4m.service.profile.model.resync.ResyncRequest;
import de.ascendro.f4m.service.profile.model.resync.ResyncResponse;
import de.ascendro.f4m.service.profile.model.sub.get.GetProfileBlobRequest;
import de.ascendro.f4m.service.profile.model.sub.get.GetProfileBlobResponse;
import de.ascendro.f4m.service.profile.model.sub.update.UpdateProfileBlobRequest;
import de.ascendro.f4m.service.profile.model.sub.update.UpdateProfileBlobResponse;
import de.ascendro.f4m.service.profile.model.update.UpdateProfileRequest;
import de.ascendro.f4m.service.profile.model.update.UpdateProfileResponse;

public class ProfileMessageTypeMapper extends JsonMessageTypeMapImpl {
	private static final long serialVersionUID = 5076486473264948406L;

	public ProfileMessageTypeMapper() {
		init();
	}

	protected void init() {
		// Create Profile
		this.register(ProfileMessageTypes.CREATE_PROFILE, new TypeToken<CreateProfileRequest>() {
		}.getType());
		this.register(ProfileMessageTypes.CREATE_PROFILE_RESPONSE, new TypeToken<CreateProfileResponse>() {
		}.getType());

		// Delete Profile
		this.register(ProfileMessageTypes.DELETE_PROFILE, new TypeToken<DeleteProfileRequest>() {
		}.getType());
		this.register(ProfileMessageTypes.DELETE_PROFILE_RESPONSE, new TypeToken<DeleteProfileResponse>() {
		}.getType());

		// Update Profile
		this.register(ProfileMessageTypes.UPDATE_PROFILE, new TypeToken<UpdateProfileRequest>() {
		}.getType());
		this.register(ProfileMessageTypes.UPDATE_PROFILE_RESPONSE, new TypeToken<UpdateProfileResponse>() {
		}.getType());

		// Find Profile
		this.register(ProfileMessageTypes.FIND_BY_IDENTIFIER, new TypeToken<FindProfileRequest>() {
		}.getType());
		this.register(ProfileMessageTypes.FIND_BY_IDENTIFIER_RESPONSE, new TypeToken<FindProfileResponse>() {
		}.getType());

		// Find Profiles by identifier
		this.register(ProfileMessageTypes.FIND_LIST_BY_IDENTIFIERS, new TypeToken<FindListByIdentifiersRequest>() {
		}.getType());
		this.register(ProfileMessageTypes.FIND_LIST_BY_IDENTIFIERS_RESPONSE, new TypeToken<FindListByIdentifiersResponse>() {
		}.getType());

		// Get Profile List by IDs
		this.register(ProfileMessageTypes.PROFILE_LIST_BY_IDS, new TypeToken<ProfileListByIdsRequest>() {
		}.getType());
		this.register(ProfileMessageTypes.PROFILE_LIST_BY_IDS_RESPONSE, new TypeToken<ProfileListByIdsResponse>() {
		}.getType());

		// Merge Profile
		this.register(ProfileMessageTypes.MERGE_PROFILE, new TypeToken<MergeProfileRequest>() {
		}.getType());
		this.register(ProfileMessageTypes.MERGE_PROFILE_RESPONSE, new TypeToken<MergeProfileResponse>() {
		}.getType());

		// Get Profile
		this.register(ProfileMessageTypes.GET_PROFILE, new TypeToken<GetProfileRequest>() {
		}.getType());
		this.register(ProfileMessageTypes.GET_PROFILE_RESPONSE, new TypeToken<GetProfileResponse>() {
		}.getType());

		// Get App Config
		this.register(ProfileMessageTypes.GET_APP_CONFIGURATION, new TypeToken<GetAppConfigurationRequest>() {
		}.getType());
		this.register(ProfileMessageTypes.GET_APP_CONFIGURATION_RESPONSE, new TypeToken<GetAppConfigurationResponse>() {
		}.getType());

		// Get Profile Blob
		this.register(ProfileMessageTypes.GET_PROFILE_BLOB, new TypeToken<GetProfileBlobRequest>() {
		}.getType());
		this.register(ProfileMessageTypes.GET_PROFILE_BLOB_RESPONSE, new TypeToken<GetProfileBlobResponse>() {
		}.getType());

		// Update/Create profile blob
		this.register(ProfileMessageTypes.UPDATE_PROFILE_BLOB, new TypeToken<UpdateProfileBlobRequest>() {
		}.getType());
		this.register(ProfileMessageTypes.UPDATE_PROFILE_BLOB_RESPONSE, new TypeToken<UpdateProfileBlobResponse>() {
		}.getType());

		// Profile Get
		this.register(ProfileMessageTypes.PROFILE_GET, new TypeToken<ProfileGetRequest>() {
		}.getType());
		this.register(ProfileMessageTypes.PROFILE_GET_RESPONSE, new TypeToken<ProfileGetResponse>() {
		}.getType());

		// Profile Update
		this.register(ProfileMessageTypes.PROFILE_UPDATE, new TypeToken<ProfileUpdateRequest>() {
		}.getType());
		this.register(ProfileMessageTypes.PROFILE_UPDATE_RESPONSE, new TypeToken<ProfileUpdateResponse>() {
		}.getType());

		// Elastic resync
		this.register(ProfileMessageTypes.RESYNC, new TypeToken<ResyncRequest>() {
		}.getType());
		this.register(ProfileMessageTypes.RESYNC_RESPONSE, new TypeToken<ResyncResponse>() {
		}.getType());

		// endConsumerInvoiceList
		this.register(ProfileMessageTypes.END_CONSUMER_INVOICE_LIST, new TypeToken<EndConsumerInvoiceListRequest>() {
		}.getType());
		this.register(ProfileMessageTypes.END_CONSUMER_INVOICE_LIST_RESPONSE, new TypeToken<EndConsumerInvoiceListResponse>() {
		}.getType());

	}
}
