package de.ascendro.f4m.service.profile.server;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.payment.model.config.InsertOrUpdateUserRequest;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.ProfileAddress;
import de.ascendro.f4m.service.profile.model.ProfileUser;
import de.ascendro.f4m.service.profile.model.api.update.ProfileUpdateRequest;
import de.ascendro.f4m.service.profile.model.get.app.GetAppConfigurationRequest;
import de.ascendro.f4m.service.profile.model.get.profile.GetProfileRequest;
import de.ascendro.f4m.service.profile.model.update.UpdateProfileRequest;
import de.ascendro.f4m.service.profile.util.ProfileUtil;
import de.ascendro.f4m.service.profile.util.ProfileUtil.ProfileUpdateInfo;
import de.ascendro.f4m.service.util.JsonLoader;
import de.ascendro.f4m.service.util.JsonTestUtil;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

public class ProfileServiceServerMessageHandlerTest {

	@Mock
	private ProfileUtil profileUtil;
	@Mock
	private ApplicationConfigurationAerospikeDao applicationConfigurationAerospikeDao;
	@InjectMocks
	private ProfileServiceServerMessageHandler handler;
	private ClientInfo clientInfo;

	private Gson gson;

	@Mock
	private GsonProvider gsonProvider;

	@Mock
	private DependencyServicesCommunicator dependencyServicesCommunicator;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		clientInfo = new ClientInfo("xxx");
		gson = JsonTestUtil.getGson();
		when(gsonProvider.get()).thenReturn(gson);
	}

	@Test
	public void testGetProfileIfNotExistsInAerospike() throws Exception {
		GetProfileRequest request = new GetProfileRequest("notExists");
		JsonMessage<GetProfileRequest> requestMessage = new JsonMessage<>(ProfileMessageTypes.GET_PROFILE, request);
		requestMessage.setClientInfo(clientInfo);
		try {
			handler.onUserMessage(requestMessage);
			fail();
		} catch (F4MEntryNotFoundException e) {
		}
	}

	@Test
	public void testSynchronizeProfileToPaymentServiceReturnIfMustWaitFromInternalCall() throws Exception {
		final Profile profile = prepareProfile();
		JsonMessage<UpdateProfileRequest> requestMessage = new JsonMessage<>(ProfileMessageTypes.UPDATE_PROFILE,
				new UpdateProfileRequest(profile.getJsonObject()));
		requestMessage.getContent().setUserId(profile.getUserId());
		requestMessage.setClientInfo(clientInfo);
		testSynchronizeProfileToPaymentService(profile, requestMessage);
	}

	@Test
	public void testSynchronizeProfileToPaymentServiceReturnIfMustWaitFromUserCall() throws Exception {
		final Profile profile = prepareProfile();
		JsonMessage<ProfileUpdateRequest> requestMessage = new JsonMessage<>(ProfileMessageTypes.PROFILE_UPDATE,
				new ProfileUpdateRequest(profile));
		requestMessage.setClientInfo(clientInfo);
		testSynchronizeProfileToPaymentService(profile, requestMessage);
	}
	
	@Test
	public void testSynchronizeProfileToPaymentServiceReturnIfMustWaitFromAppConfig() throws Exception {
		when(applicationConfigurationAerospikeDao.getAppConfigurationAsJsonElement(KeyStoreTestUtil.TENANT_ID, null)).thenReturn(new JsonObject());
		final Profile profile = prepareProfile();
		when(profileUtil.getActiveProfile(clientInfo.getUserId())).thenReturn(profile);
		when(profileUtil.updateProfileStatistics(any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(new ArrayList<>(profile.getTenants()));

		GetAppConfigurationRequest request = new GetAppConfigurationRequest();
		request.setTenantId(KeyStoreTestUtil.TENANT_ID);
		JsonMessage<GetAppConfigurationRequest> requestMessage = new JsonMessage<>(ProfileMessageTypes.GET_APP_CONFIGURATION,
				request);


		requestMessage.setClientInfo(clientInfo);
		testSynchronizeProfileToPaymentService(profile, requestMessage);
	}
	
	@Test
	public void testIsFullyRegisteredRole() throws Exception {
		Profile profile = new Profile();
		assertFalse(handler.isFullyRegisteredRole(profile));
		
		// set mandatory person properties
		ProfileUser person = new ProfileUser();
		person.setFirstName("FirstName");
		person.setLastName("Last name");
		person.setNickname("Nickname");
		profile.setPersonWrapper(person);
		
		// set mandatory address properties
		ProfileAddress address = new ProfileAddress();
		address.setCountry("Country");
		address.setCity("City");
		profile.setAddress(address);
		
		assertTrue(handler.isFullyRegisteredRole(profile));
	}
	
	private Profile prepareProfile() throws IOException {
		final String sourceJsonString = JsonLoader.getTextFromResources("sourceProfile.json", ProfileUtil.class);
		return new Profile(gson.fromJson(sourceJsonString, JsonElement.class));
	}
	
	private void testSynchronizeProfileToPaymentService(Profile profile, JsonMessage<?> requestMessage) throws Exception {
		ProfileUpdateInfo profileUpdateInfo = new ProfileUpdateInfo();
		profileUpdateInfo.setProfile(profile);
		when(profileUtil.updateProfile(anyString(), any(), anyBoolean(), anyBoolean(), anyBoolean()))
				.thenReturn(new ImmutablePair<>(profileUpdateInfo, new Profile()));

		handler.onUserMessage(requestMessage);

		ArgumentCaptor<InsertOrUpdateUserRequest> message = ArgumentCaptor.forClass(InsertOrUpdateUserRequest.class);
		verify(dependencyServicesCommunicator, times(2)).synchronizeProfileToPaymentService(message.capture(), any());

		List<InsertOrUpdateUserRequest> values = message.getAllValues();
		verifyTenantInfoInSyncCall(profile, "tenantId1", values.get(0));
		verifyTenantInfoInSyncCall(profile, "tenantId2", values.get(1));
	}

	private void verifyTenantInfoInSyncCall(final Profile profile, String tenantId, InsertOrUpdateUserRequest request) {
		assertThat(request.getTenantId(), equalTo(tenantId));
		assertThat(request.getProfileId(), equalTo("5555"));
		assertThat(request.getProfile(), sameInstance(profile.getJsonObject()));
	}
}
