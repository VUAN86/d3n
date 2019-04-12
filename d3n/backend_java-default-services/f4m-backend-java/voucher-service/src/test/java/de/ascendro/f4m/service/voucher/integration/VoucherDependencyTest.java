package de.ascendro.f4m.service.voucher.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.gson.JsonArray;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;

import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.event.EventMessageTypes;
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
import de.ascendro.f4m.service.util.ProfileServiceTestHelper;
import de.ascendro.f4m.service.voucher.di.VoucherDefaultMessageMapper;
import de.ascendro.f4m.service.voucher.model.schema.VoucherMessageSchemaMapper;

public class VoucherDependencyTest extends F4MServiceWithMockIntegrationTestBase {

	private static final String SOURCE_PROFILE_ID = "1_2";
	private static final String TARGET_PROFILE_ID = "1_3";

	private CommonProfileAerospikeDao profileInstanceAerospikeDao;
	protected JsonMessageUtil jsonMessageUtil;

	@Override
	protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock() {
		return ctx -> onReceivedMessage(ctx.getMessage());
	}

	@Override
	protected void configureInjectionModuleMock(BindableAbstractModule module) {
		module.bindToModule(JsonMessageTypeMap.class).to(VoucherDefaultMessageMapper.class);
		module.bindToModule(JsonMessageSchemaMap.class).to(VoucherMessageSchemaMapper.class);
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		jsonMessageUtil = clientInjector.getInstance(JsonMessageUtil.class);
		Injector injector = jettyServerRule.getServerStartup().getInjector();
		assertServiceStartup(EventMessageTypes.SERVICE_NAME);
		profileInstanceAerospikeDao = injector.getInstance(CommonProfileAerospikeDao.class);
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new VoucherServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE) {
			@Override
			protected AbstractModule getVoucherServiceAerospikeOverrideModule() {
				return new AbstractModule() {
					@Override
					protected void configure() {
						CommonProfileAerospikeDao profileInstanceAerospikeDao = mock(CommonProfileAerospikeDao.class);
						bind(CommonProfileAerospikeDao.class).toInstance(profileInstanceAerospikeDao);
					}
				};
			}
		};
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(VoucherDefaultMessageMapper.class, VoucherMessageSchemaMapper.class);
	}

	private JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
			throws Exception {
		return null;
	}

	@Test
	public void testUserVoucherGet() throws Exception {
		when(profileInstanceAerospikeDao.getVoucherReferencesArrayFromBlob(any())).thenReturn(new JsonArray());
		sendAsynMessageAsMockClient(ProfileServiceTestHelper.createMergeProfileMessage(jsonMessageUtil,
				SOURCE_PROFILE_ID, TARGET_PROFILE_ID));
		ArgumentCaptor<String> sourceArgument = ArgumentCaptor.forClass(String.class);
		RetriedAssert.assertWithWait(
				() -> verify(profileInstanceAerospikeDao).getVoucherReferencesArrayFromBlob(sourceArgument.capture()));
		assertThat(sourceArgument.getValue(), equalTo(SOURCE_PROFILE_ID));
	}
}