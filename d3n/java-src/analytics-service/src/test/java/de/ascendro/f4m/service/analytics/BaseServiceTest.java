package de.ascendro.f4m.service.analytics;

import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.ConsistencyLevel;
import com.aerospike.client.policy.ScanPolicy;
import com.google.inject.Injector;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.analytics.AnalyticsDaoImpl;
import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.server.profile.util.ProfilePrimaryKeyUtil;
import de.ascendro.f4m.server.util.JsonUtil;
import de.ascendro.f4m.service.analytics.notification.di.AnalyticsDefaultMessageTypeMapper;
import de.ascendro.f4m.service.analytics.notification.schema.AnalyticsMessageSchemaMapper;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.integration.rule.MockServiceRule;
import de.ascendro.f4m.service.integration.test.F4MServiceWithMockIntegrationTestBase;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.tombola.config.TombolaConfig;
import de.ascendro.f4m.service.usermessage.model.SendWebsocketMessageRequest;
import de.ascendro.f4m.service.util.register.ServiceMonitoringRegister;

public abstract class BaseServiceTest extends F4MServiceWithMockIntegrationTestBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseServiceTest.class);
    protected ServiceMonitoringRegister serviceMonitoringRegister;
    protected AerospikeClientProvider aerospikeClientProvider;
    protected String profileSet;
    protected Config config;
    protected JsonUtil jsonUtil;
    protected ProfilePrimaryKeyUtil profilePrimaryKeyUtil;

    protected AnalyticsServiceStartup analyticsServiceStartup;
    protected Injector injector ;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        analyticsServiceStartup = ((AnalyticsServiceStartup)jettyServerRule.getServerStartup());
        injector = analyticsServiceStartup.getInjector();

        jsonUtil = new JsonUtil();

        profileSet = config.getProperty(AerospikeConfigImpl.AEROSPIKE_PROFILE_SET);
        profilePrimaryKeyUtil = new ProfilePrimaryKeyUtil(config);
    }

    @After
    public void tearDown() throws Exception {
        if (aerospikeClientProvider != null && !(aerospikeClientProvider instanceof AerospikeClientMockProvider)) {
            try {
                clearTestAnalyticsInstanceSet();
            } finally {
                if (aerospikeClientProvider != null) {
                    aerospikeClientProvider.get()
                            .close();
                }
            }
        }
    }

    @Override
    protected void configureInjectionModuleMock(MockServiceRule.BindableAbstractModule module) {
        module.bindToModule(JsonMessageTypeMap.class).to(AnalyticsDefaultMessageTypeMapper.class);
        module.bindToModule(JsonMessageSchemaMap.class).to(AnalyticsMessageSchemaMapper.class);
    }

    @Override
    protected MockServiceRule.ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock() {
        return ctx -> onReceivedMessage(ctx.getMessage());
    }

    @Override
    protected TestClientInjectionClasses getTestClientInjectionClasses() {
        return new TestClientInjectionClasses(AnalyticsDefaultMessageTypeMapper.class, AnalyticsMessageSchemaMapper.class);
    }

    protected abstract JsonMessageContent onReceivedMessage(JsonMessage<? extends JsonMessageContent> originalMessageDecoded)
            throws Exception;

    protected void clearTestAnalyticsInstanceSet() {
        LOGGER.info("Clearing Aerospike set {} within namespace {} at exit", AnalyticsDaoImpl.ANALYTICS_SET_NAME, AnalyticsTestConfig.NAMESPACE);
        clearSet(AnalyticsTestConfig.NAMESPACE, AnalyticsDaoImpl.ANALYTICS_SET_NAME);
        clearSet(AnalyticsTestConfig.NAMESPACE, profileSet);
        clearSet(AnalyticsTestConfig.NAMESPACE, new TombolaConfig().getProperty(TombolaConfig.AEROSPIKE_TOMBOLA_SET));
    }

    protected void clearSet(String namespace, String set) {
        LOGGER.info("Clearing set[{}] within namesapce[{}]", set, namespace);
        final IAerospikeClient iAerospikeClient = aerospikeClientProvider.get();
        final ScanPolicy scanPolicy = new ScanPolicy();
        scanPolicy.consistencyLevel = ConsistencyLevel.CONSISTENCY_ALL;
        iAerospikeClient.scanAll(scanPolicy, namespace, set, (key, record) -> iAerospikeClient.delete(null, key));
        LOGGER.info("Done clearing set[{}] within namesapce[{}]", set, namespace);
    }

    private void setUpRealAerospikeClientProvider() {
        initServiceMonitoringRegister();
        aerospikeClientProvider = new AerospikeClientProvider(config, serviceMonitoringRegister);
    }

    private void setUpMockAerospikeClientProvider() {
        initServiceMonitoringRegister();
        aerospikeClientProvider = new AerospikeClientMockProvider(config, serviceMonitoringRegister);
    }

    private void initServiceMonitoringRegister() {
        serviceMonitoringRegister = new ServiceMonitoringRegister();
    }

    protected Config createConfig() {
        return new AnalyticsTestConfig();
    }

    @Override
    protected AnalyticsServiceStartup getServiceStartup() {
        config = createConfig();

        if(!StringUtils.isEmpty(config.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST))){
            setUpRealAerospikeClientProvider();
        }else{
            setUpMockAerospikeClientProvider();
        }

        assertTrue(aerospikeClientProvider.get()
                .isConnected());

        if (StringUtils.isEmpty(config.getProperty(AerospikeConfigImpl.AEROSPIKE_SERVER_HOST))) {
            return getAnalyticsServiceStartupUsingAerospikeMock();
        } else {
            return getAnalyticsServiceStartupUsingAerospikeReal();
        }
    }

    protected SendWebsocketMessageRequest getPushRequestWrapper(String messageContent,
                                                      String toUserID, String[] parameters) {
        SendWebsocketMessageRequest pushWrapperRequest = new SendWebsocketMessageRequest(true);
        pushWrapperRequest.setUserId(toUserID);
        pushWrapperRequest.setMessage(messageContent);
        if (parameters!=null) {
            pushWrapperRequest.setParameters(parameters);
        }

        return pushWrapperRequest;
    }

    protected abstract AnalyticsServiceStartupUsingAerospikeMock getAnalyticsServiceStartupUsingAerospikeMock();

    protected abstract AnalyticsServiceStartupUsingAerospikeReal getAnalyticsServiceStartupUsingAerospikeReal();
}
