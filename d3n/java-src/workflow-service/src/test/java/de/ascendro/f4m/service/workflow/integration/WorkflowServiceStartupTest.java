package de.ascendro.f4m.service.workflow.integration;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import javax.inject.Singleton;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.annotation.ClientMessageHandler;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.di.handler.JsonMessageHandlerProvider;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.event.model.publish.PublishMessageContent;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.integration.collector.ReceivedMessageCollector;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.BindableAbstractModule;
import de.ascendro.f4m.service.integration.rule.MockServiceRule.ThrowingFunction;
import de.ascendro.f4m.service.integration.test.ServiceStartupTest;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageSchemaMap;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMap;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringConnectionStatus;
import de.ascendro.f4m.service.registry.model.monitor.MonitoringDbConnectionInfo;
import de.ascendro.f4m.service.session.SessionWrapper;
import de.ascendro.f4m.service.util.MonitoringDbConnectionInfoBuilder;
import de.ascendro.f4m.service.util.register.ServiceRegistryClient;
import de.ascendro.f4m.service.workflow.config.WorkflowConfig;
import de.ascendro.f4m.service.workflow.di.WorkflowDefaultMessageTypeMapper;
import de.ascendro.f4m.service.workflow.model.perform.PerformResponse;
import de.ascendro.f4m.service.workflow.model.schema.WorkflowMessageSchemaMapper;
import de.ascendro.f4m.service.workflow.model.start.StartResponse;
import de.ascendro.f4m.service.workflow.model.state.StateResponse;
import de.ascendro.f4m.service.workflow.utils.WorkflowResourceProvider;
import de.ascendro.f4m.service.workflow.utils.WorkflowWrapper;

public class WorkflowServiceStartupTest extends ServiceStartupTest {

	private ReceivedMessageCollector receivedMessageCollector;
	private ServiceStartup serviceStartup;

	@Override
	protected void configureInjectionModuleMock(BindableAbstractModule module) {
		module.bindToModule(JsonMessageTypeMap.class).to(WorkflowDefaultMessageTypeMapper.class);
		module.bindToModule(JsonMessageSchemaMap.class).to(WorkflowMessageSchemaMapper.class);
	}

	@Override
	protected ThrowingFunction<RequestContext, JsonMessageContent> getMessageHandlerMock() {
		return ctx -> onReceivedMessage(ctx);
	}

    private ReceivedMessageCollector dummyServiceMessageCollector = new ReceivedMessageCollector(){
        @Override
		public SessionWrapper getSessionWrapper() {
            return Mockito.mock(SessionWrapper.class);
        };
    };

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		MockitoAnnotations.initMocks(this);
		receivedMessageCollector = (ReceivedMessageCollector) clientInjector.getInstance(
				Key.get(JsonMessageHandlerProvider.class, ClientMessageHandler.class)).get();
	}

	@After
	public void tearDown() throws Exception {
		WorkflowWrapper workflowWrapper = jettyServerRule.getServerStartup().getInjector().getInstance(WorkflowWrapper.class);
		workflowWrapper.close();
	}
	
	@Override
	public Class<? extends F4MConfigImpl> getServiceConfigClass() {
		return WorkflowConfig.class;
	}

	@Override
	protected MonitoringDbConnectionInfo getExpectedDbStatus() {
		return new MonitoringDbConnectionInfoBuilder(super.getExpectedDbStatus())
				.aerospike(MonitoringConnectionStatus.NC).build();
	}

	@Test
	public void testStartTaskAndDoAction() throws Exception {
		//Emulate service registry response for event service get message
		final String getEventServiceResponseJson = getPlainTextJsonFromResources("/integration/register/getEventServiceResponse.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getEventServiceResponseJson);

		final ServiceRegistryClient serviceRegistryClient = jettyServerRule.getServerStartup().getInjector()
				.getInstance(ServiceRegistryClient.class);
		RetriedAssert.assertWithWait(() -> assertNotNull(serviceRegistryClient.getServiceStore().getService(
				EventMessageTypes.SERVICE_NAME)));

		// Start task
		final String startTaskSimpleJson = getPlainTextJsonFromResources("startTaskSimple.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), startTaskSimpleJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> startTaskResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		
		assertMessageContentType(startTaskResponseMessage, StartResponse.class);
		StartResponse startTaskResponse = (StartResponse) startTaskResponseMessage.getContent();
		assertThat(startTaskResponse.getAvailableActionTypes(), containsInAnyOrder("drink beer", "review"));
		assertFalse(startTaskResponse.isProcessFinished());
		
		RetriedAssert.assertWithWait(() -> assertEquals(2, dummyServiceMessageCollector.getReceivedMessageList().size()));
		JsonMessage<? extends JsonMessageContent> userTaskTriggeredMessage = dummyServiceMessageCollector.getReceivedMessageList().get(0);
		assertMessageContentType(userTaskTriggeredMessage, PublishMessageContent.class);
		PublishMessageContent publishContent = (PublishMessageContent) userTaskTriggeredMessage.getContent();
		if (publishContent.getNotificationContent().getAsJsonObject().get("triggeredAction").getAsString().equals("drink beer")) {
			// Can occur in any order
			userTaskTriggeredMessage = dummyServiceMessageCollector.getReceivedMessageList().get(1);
			assertMessageContentType(userTaskTriggeredMessage, PublishMessageContent.class);
			publishContent = (PublishMessageContent) userTaskTriggeredMessage.getContent();
		}
		assertEquals("workflow/*", publishContent.getTopic());
		assertEquals("review", publishContent.getNotificationContent().getAsJsonObject().get("triggeredAction").getAsString());
		
		receivedMessageCollector.clearReceivedMessageList();
		dummyServiceMessageCollector.clearReceivedMessageList();
		
		// Do action
		final String doActionJson = getPlainTextJsonFromResources("doActionSimple.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), doActionJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> doActionResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		
		assertMessageContentType(doActionResponseMessage, PerformResponse.class);
		PerformResponse doActionResponse = (PerformResponse) doActionResponseMessage.getContent();
		assertEquals("review", doActionResponse.getTriggeredActionType());
		assertThat(doActionResponse.getAvailableActionTypes(), contains("drink beer"));
		assertFalse(doActionResponse.isProcessFinished());

		receivedMessageCollector.clearReceivedMessageList();
		dummyServiceMessageCollector.clearReceivedMessageList();
		
		// Check status
		final String getStateJson = getPlainTextJsonFromResources("getState.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getStateJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		JsonMessage<? extends JsonMessageContent> getStateResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		
		assertMessageContentType(getStateResponseMessage, StateResponse.class);
		StateResponse getStateResponse = (StateResponse) getStateResponseMessage.getContent();
		assertThat(getStateResponse.getAvailableActionTypes(), contains("drink beer"));
		assertFalse(getStateResponse.isProcessFinished());

		receivedMessageCollector.clearReceivedMessageList();
		dummyServiceMessageCollector.clearReceivedMessageList();

		// Abort process
		final String abortJson = getPlainTextJsonFromResources("abort.json");
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), abortJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		final JsonMessage<? extends JsonMessageContent> abortResponseMessage = receivedMessageCollector
				.getReceivedMessageList().get(0);
		
		assertMessageContentType(abortResponseMessage, EmptyJsonMessageContent.class);

		receivedMessageCollector.clearReceivedMessageList();
		dummyServiceMessageCollector.clearReceivedMessageList();

		// Check status
		jsonWebSocketClientSessionPool.sendAsyncText(getServiceConnectionInformation(), getStateJson);
		RetriedAssert.assertWithWait(() -> assertEquals(1, receivedMessageCollector.getReceivedMessageList().size()));
		getStateResponseMessage = receivedMessageCollector.getReceivedMessageList().get(0);
		
		assertMessageContentType(getStateResponseMessage, StateResponse.class);
		getStateResponse = (StateResponse) getStateResponseMessage.getContent();
		assertTrue(getStateResponse.getAvailableActionTypes().isEmpty());
		assertTrue(getStateResponse.isProcessFinished());
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		serviceStartup = new WorkflowServiceStartupUsingAerospikeMock(DEFAULT_TEST_STAGE) {
			@Override
			protected Iterable<? extends Module> getModules() {
		        return Arrays.asList(new AbstractModule() {
		            @Override
		            protected void configure() {
		                bind(AerospikeClientProvider.class).to(AerospikeClientMockProvider.class).in(Singleton.class);
		                bind(WorkflowResourceProvider.class).to(TestWorkflowResourceProvider.class).in(Singleton.class);
		            }
		        });
			}
		};
		return serviceStartup;
	}
	
	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(WorkflowDefaultMessageTypeMapper.class, WorkflowMessageSchemaMapper.class);
	}

	public static class TestWorkflowResourceProvider implements WorkflowResourceProvider {
		@Override
		public List<Resource> getResources() {
			return Arrays.asList(ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/utils/splitFlowTest.bpmn", getClass()));
		}
	}
	
    private JsonMessageContent onReceivedMessage(RequestContext ctx) throws Exception {
        dummyServiceMessageCollector.onProcess(ctx);
        return super.onMockReceivedMessage(ctx.getMessage());
    }

}