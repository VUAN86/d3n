package de.ascendro.f4m.service.winning.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.inject.Injector;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
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
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.util.ProfileServiceTestHelper;
import de.ascendro.f4m.service.winning.dao.UserWinningComponentAerospikeDao;
import de.ascendro.f4m.service.winning.di.WinningDefaultMessageMapper;
import de.ascendro.f4m.service.winning.model.schema.WinningMessageSchemaMapper;

public class WinningDependencyTest extends F4MServiceWithMockIntegrationTestBase {

	private static final String SOURCE_PROFILE_ID = "1_2";
	private static final String TARGET_PROFILE_ID = "1_3";

	private UserWinningComponentAerospikeDao userWinningComponentAerospikeDao;
	private CommonProfileAerospikeDao profileDao;
	private ProfilePrimaryKeyUtil profilePrimaryKeyUtil;
	protected JsonMessageUtil jsonMessageUtil;

	@Override
	protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock() {
		return ctx -> onReceivedMessage(ctx.getMessage());
	}

	@Override
	protected void configureInjectionModuleMock(BindableAbstractModule module) {
		module.bindToModule(JsonMessageTypeMap.class).to(WinningDefaultMessageMapper.class);
		module.bindToModule(JsonMessageSchemaMap.class).to(WinningMessageSchemaMapper.class);
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		jsonMessageUtil = clientInjector.getInstance(JsonMessageUtil.class);
		Injector injector = jettyServerRule.getServerStartup().getInjector();
		assertServiceStartup(EventMessageTypes.SERVICE_NAME);
		userWinningComponentAerospikeDao = injector.getInstance(UserWinningComponentAerospikeDao.class);
		profileDao = injector.getInstance(CommonProfileAerospikeDao.class);
		profilePrimaryKeyUtil = injector.getInstance(ProfilePrimaryKeyUtil.class);
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return new WinningServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE);
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(WinningDefaultMessageMapper.class, WinningMessageSchemaMapper.class);
	}

	private JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
			throws Exception {
		return null;
	}

	@Test
	public void testUserWinningGet() throws Exception {
		createProfile(SOURCE_PROFILE_ID);
		
		sendAsynMessageAsMockClient(ProfileServiceTestHelper.createMergeProfileMessage(jsonMessageUtil,
				SOURCE_PROFILE_ID, TARGET_PROFILE_ID));

		ArgumentCaptor<String> sourceArgument = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> targetArgument = ArgumentCaptor.forClass(String.class);
		RetriedAssert.assertWithWait(() -> verify(userWinningComponentAerospikeDao)
				.moveUserWinningComponents(anyString(), sourceArgument.capture(), targetArgument.capture()));
		assertThat(sourceArgument.getValue(), equalTo(SOURCE_PROFILE_ID));
		assertThat(targetArgument.getValue(), equalTo(TARGET_PROFILE_ID));
	}

	private void createProfile(String userId) {
		Profile profile = new Profile();
		profile.setUserId(userId);
		profile.addApplication("appId");
		
		String key = profilePrimaryKeyUtil.createPrimaryKey(profile.getUserId());
		String set = config.getProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET);
		profileDao.createJson(set, key, CommonProfileAerospikeDao.BLOB_BIN_NAME, profile.getAsString());
	}

}
