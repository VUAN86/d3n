package de.ascendro.f4m.service.usermessage.integration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.Bin;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.google.inject.Injector;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.AerospikeDaoImpl;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDaoImpl;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.util.ApplicationConfigrationPrimaryKeyUtil;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.BindableAbstractModule;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.ThrowingFunction;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.profile.ProfileMessageTypes;
import de.ascendro.f4m.service.registry.ServiceRegistryMessageTypes;
import de.ascendro.f4m.service.usermessage.di.UserMessageDefaultMessageTypeMapper;
import de.ascendro.f4m.service.usermessage.model.schema.UserMessageMessageSchemaMapper;
import de.ascendro.f4m.service.util.ProfileServiceTestHelper;

public abstract class UserMessageServiceApiTestBase extends F4MServiceWithMockIntegrationTestBase {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserMessageServiceApiTestBase.class);

	@Override
	protected void configureInjectionModuleMock(BindableAbstractModule module) {
		module.bindToModule(JsonMessageTypeMap.class).to(UserMessageDefaultMessageTypeMapper.class);
		module.bindToModule(JsonMessageSchemaMap.class).to(UserMessageMessageSchemaMapper.class);
	};

	@Override
	protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock() {
		return ctx -> onReceivedMessage(ctx.getMessage());
	}

	protected JsonMessageUtil jsonUtil;
	private IAerospikeClient aerospikeClient;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		assertServiceStartup(ProfileMessageTypes.SERVICE_NAME);

		Injector serverInjector = jettyServerRule.getServerStartup().getInjector();
		jsonUtil = serverInjector.getInstance(JsonMessageUtil.class);
		
		CommonProfileAerospikeDao profileAerospikeDao = serverInjector.getInstance(CommonProfileAerospikeDao.class);
		when(profileAerospikeDao.getProfile(anyString())).thenReturn(ProfileServiceTestHelper.getTestProfile());
		
		aerospikeClient = jettyServerRule.getServerStartup().getInjector().getInstance(AerospikeClientProvider.class).get();
		ApplicationConfigrationPrimaryKeyUtil applicationConfigrationPrimaryKeyUtil = serverInjector
				.getInstance(ApplicationConfigrationPrimaryKeyUtil.class);
		String appConfigId = applicationConfigrationPrimaryKeyUtil.createPrimaryKey("tenant-1", "appId-1");
		Key appConfigKey = new Key("test",
				serverInjector.getInstance(ApplicationConfigurationAerospikeDaoImpl.class).getSet(), appConfigId);
		Bin appConfigBin = new Bin(ApplicationConfigurationAerospikeDao.BLOB_BIN_NAME,
				"{}".getBytes(AerospikeDaoImpl.DEFAULT_CHARSET_AEROSPIKE));
		aerospikeClient.put(null, appConfigKey, appConfigBin);
	}
	
	@After
	public void clearAerospikeData() {
		this.aerospikeClient.close();
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(UserMessageDefaultMessageTypeMapper.class, UserMessageMessageSchemaMapper.class);
	}

	protected JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
			throws Exception {
		LOGGER.debug("Mocked Profile service received request {}", originalMessageDecoded.getContent());
		if (originalMessageDecoded.getName().toUpperCase()
				.contains(ServiceRegistryMessageTypes.SERVICE_NAME.toUpperCase())) {
			LOGGER.debug("Service Registry message[{}] recived within mock service: {}",
					originalMessageDecoded.getName(), originalMessageDecoded.getContent());
			return null;//Ignore Service registry calls
		} else {
			throw new UnexpectedTestException("Unexpected message: " + originalMessageDecoded.getName()
					+ " originalMessageDecoded: [" + originalMessageDecoded.getContent() + "]");
		}
	}
}