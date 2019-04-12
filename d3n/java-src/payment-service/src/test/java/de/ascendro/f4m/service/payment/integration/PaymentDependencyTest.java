package de.ascendro.f4m.service.payment.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.inject.Injector;

import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.integration.F4MAssert;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.BindableAbstractModule;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.ThrowingFunction;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.payment.client.DependencyServicesCommunicator;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.di.PaymentDefaultMessageTypeMapper;
import de.ascendro.f4m.service.payment.manager.PaymentManager;
import de.ascendro.f4m.service.payment.manager.impl.PaymentManagerImpl;
import de.ascendro.f4m.service.payment.manager.impl.PaymentManagerMockImpl;
import de.ascendro.f4m.service.payment.model.config.MergeUsersRequest;
import de.ascendro.f4m.service.payment.model.schema.PaymentMessageSchemaMapper;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.profile.model.update.UpdateProfileResponse;
import de.ascendro.f4m.service.util.ProfileServiceTestHelper;

public class PaymentDependencyTest extends F4MServiceWithMockIntegrationTestBase {

	private static final String TENANT_ID = "1";
	private static final String SOURCE_PROFILE_ID = "2";
	private static final String TARGET_PROFILE_ID = "3";

	protected PaymentManager paymentManager;
	protected JsonMessageUtil jsonMessageUtil;
	protected CommonProfileAerospikeDao commonProfileAerospikeDao;
	private List<JsonMessage<? extends JsonMessageContent>> receivedMessages = new CopyOnWriteArrayList<>();

	@Override
	protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock() {
		return ctx -> onReceivedMessage(ctx.getMessage());
	}

	@Override
	protected void configureInjectionModuleMock(BindableAbstractModule module) {
		module.bindToModule(JsonMessageTypeMap.class).to(PaymentDefaultMessageTypeMapper.class);
		module.bindToModule(JsonMessageSchemaMap.class).to(PaymentMessageSchemaMapper.class);
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		config.setProperty(PaymentConfig.MOCK_MODE, true);
		jsonMessageUtil = clientInjector.getInstance(JsonMessageUtil.class);
		final Injector paymentServiceInjector = jettyServerRule.getServerStartup().getInjector();
		assertServiceStartup(EventMessageTypes.SERVICE_NAME);
		paymentManager = paymentServiceInjector.getInstance(PaymentManager.class);
		commonProfileAerospikeDao = paymentServiceInjector.getInstance(CommonProfileAerospikeDao.class);
		
		assertFalse(paymentManager instanceof PaymentManagerImpl);
		assertFalse(paymentManager instanceof PaymentManagerMockImpl);
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new PaymentServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE);
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(PaymentDefaultMessageTypeMapper.class, PaymentMessageSchemaMapper.class);
	}

	@Test
	public void testMessageFromEventService() throws Exception {
		Profile profile = new Profile();
		profile.addTenant("1");
		when(commonProfileAerospikeDao.getProfile(any())).thenReturn(profile);

		sendAsynMessageAsMockClient(ProfileServiceTestHelper.createMergeProfileMessage(jsonMessageUtil,
				SOURCE_PROFILE_ID, TARGET_PROFILE_ID));

		ArgumentCaptor<MergeUsersRequest> argumentCaptor = ArgumentCaptor.forClass(MergeUsersRequest.class);
		RetriedAssert.assertWithWait(() -> verify(paymentManager).mergeUserAccounts(argumentCaptor.capture()));
		assertThat(argumentCaptor.getValue().getTenantId(), equalTo(TENANT_ID));
		assertThat(argumentCaptor.getValue().getFromProfileId(), equalTo(SOURCE_PROFILE_ID));
		assertThat(argumentCaptor.getValue().getToProfileId(), equalTo(TARGET_PROFILE_ID));
	}
	
	@Test
	public void testProfileDependency() throws Exception {
		//TODO: Fix guice setup to return same instance if declared as singleton - fix warning "WARNING: Multiple Servlet injectors detected."
		DependencyServicesCommunicator communicator = jettyServerRule.getServerStartup().getInjector()
				.getInstance(DependencyServicesCommunicator.class);
		communicator.requestUpdateProfileIdentity("userId", new Profile());
		RetriedAssert.assertWithWait(() -> F4MAssert.assertReceivedMessagesAnyOrderWithWait(receivedMessages,
				ProfileMessageTypes.UPDATE_PROFILE));
		assertNotNull(receivedMessages.get(0).getContent());
	}

	@Test
	public void mergeProfilesWrongUserTest() throws Exception {
		when(commonProfileAerospikeDao.getProfile(any())).thenReturn(new Profile());
		sendAsynMessageAsMockClient(ProfileServiceTestHelper.createMergeProfileMessage(jsonMessageUtil,
				SOURCE_PROFILE_ID, TARGET_PROFILE_ID));
		RetriedAssert.assertWithWait(() -> errorCollector.setExpectedErrors(new F4MValidationFailedException(
				"Unexpected error message JsonMessageError[server : ERR_FATAL_ERROR]")));
	}

	protected JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
			throws Exception {
		if (ProfileMessageTypes.UPDATE_PROFILE.getMessageName().equals(originalMessageDecoded.getName())) {
			receivedMessages.add(originalMessageDecoded);
			return new UpdateProfileResponse();
		} else {
			return null;
		}
	}

}