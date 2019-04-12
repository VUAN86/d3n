package de.ascendro.f4m.service.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.client.json.JsonWebSocketClientSessionPool;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.config.F4MConfig;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.exception.ExceptionCodes;
import de.ascendro.f4m.service.exception.ExceptionType;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.integration.rule.JettyServerRule;
import de.ascendro.f4m.service.integration.test.F4MServiceIntegrationTestBase;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.json.RequestContext;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.ping.PingPongMessageSchemaMapper;
import de.ascendro.f4m.service.ping.PingPongMessageTypeMapper;
import de.ascendro.f4m.service.ping.PingPongServiceStartup;
import de.ascendro.f4m.service.ping.model.JsonPingMessageContent;
import de.ascendro.f4m.service.ping.model.JsonPongMessageContent;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.request.RequestInfo;
import de.ascendro.f4m.service.request.RequestInfoImpl;
import de.ascendro.f4m.service.session.SessionWrapper;

/**
 * Verifies following communication cases:
 * client => ping-pong service => dependent service => ping-pong service => client
 */
@SuppressWarnings("unchecked")
public class ErrorMessageForwardingTest extends F4MServiceIntegrationTestBase {
	private static final Logger LOGGER = LoggerFactory.getLogger(ErrorMessageForwardingTest.class);

	private JsonMessageUtil jsonMessageUtil;
	private PingPongServiceStartup pingPongServiceStartup;
	private PingPongServiceStartup dependentServiceStartup;

	@Rule
	public JettyServerRule mockProfileService = new JettyServerRule(createDependentServiceStartup(), 8437);

	@Override
	public void setUp() throws Exception {
		super.setUp();
		jsonMessageUtil = jettyServerRule.getServerStartup().getInjector().getInstance(JsonMessageUtil.class);
	}
	
	@Override
	protected ServiceStartup getServiceStartup() {
		if (pingPongServiceStartup == null) {
			pingPongServiceStartup = new PingPongServiceStartup("PING_SERVICE");
		}
		return pingPongServiceStartup;
	}

	private ServiceStartup createDependentServiceStartup() {
		if (dependentServiceStartup == null) {
			dependentServiceStartup = new PingPongServiceStartup("DEPENDENT_SERVICE") {
				@Override
				protected List<String> getDependentServiceNames() {
					return Arrays.asList("test");
				}
			};
		}
		return dependentServiceStartup;
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(PingPongMessageTypeMapper.class, PingPongMessageSchemaMapper.class);
	}

	@Test
	public void testSuccessfulForwarding() throws Exception {
		pingPongServiceStartup.getServerHandlerProvider().setHandler((m, s) -> onMessageRegisterInCacheAndForwardToDependentService(m, s));
		pingPongServiceStartup.getClientHandlerProvider().setHandler((m, s) -> onMessageForwardResponseToOriginalService(m));
		dependentServiceStartup.getServerHandlerProvider().setHandler((m,s) -> respondWithPong(m));
		
		JsonMessage<JsonMessageContent> ping = new JsonMessage<>(JsonPingMessageContent.MESSAGE_NAME);
		ping.setSeq(1L);
		ping.setContent(new JsonPingMessageContent("Ping from client"));
		jsonWebSocketClientSessionPool.sendAsyncMessage(getServiceConnectionInformation(), ping);
		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		JsonMessage<JsonPongMessageContent> pong = (JsonMessage<JsonPongMessageContent>) testClientReceivedMessageCollector
				.getReceivedMessageList().get(0);
		assertNotNull(pong.getSeq());
		assertThat(pong.getName(), equalTo(JsonPongMessageContent.MESSAGE_NAME));
		assertThat(pong.getFirstAck(), equalTo(ping.getSeq()));
		assertNotNull(pong.getContent());
		assertThat(pong.getContent().getPong(), equalTo("Forwarded response : Response from dependent service on 'Forwarded info : Ping from client'"));
	}
	
	@Test
	public void testSuccessfulErrorForwardingOnDependentServiceFailure() throws Exception {
		pingPongServiceStartup.getServerHandlerProvider().setHandler((m, s) -> onMessageRegisterInCacheAndForwardToDependentService(m, s));
		dependentServiceStartup.getServerHandlerProvider().setHandler((m,s) -> throwExceptionOnMessage(m, "Dependent service failed"));
		
		JsonMessage<JsonMessageContent> ping = new JsonMessage<>(JsonPingMessageContent.MESSAGE_NAME);
		ping.setSeq(1L);
		ping.setContent(new JsonPingMessageContent("Ping from client"));
		jsonWebSocketClientSessionPool.sendAsyncMessage(getServiceConnectionInformation(), ping);
		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		JsonMessage<JsonPongMessageContent> pong = (JsonMessage<JsonPongMessageContent>) testClientReceivedMessageCollector
				.getReceivedMessageList().get(0);
		assertNotNull(pong.getSeq());
		assertThat(pong.getName(), equalTo(JsonPongMessageContent.MESSAGE_NAME));
		assertThat(pong.getFirstAck(), equalTo(ping.getSeq()));
		assertNotNull(pong.getError());
		assertThat(pong.getError().getCode(), equalTo(ExceptionCodes.ERR_ENTRY_NOT_FOUND));		
		assertThat(pong.getError().getType(), equalTo(ExceptionType.CLIENT.name().toLowerCase()));
	}
	
	@Test
	public void testSuccessfulErrorForwardingOnServiceClientHandlerFailure() throws Exception {
		pingPongServiceStartup.getServerHandlerProvider().setHandler((m, s) -> onMessageRegisterInCacheAndForwardToDependentService(m, s));
		pingPongServiceStartup.getClientHandlerProvider().setHandler((m,s) -> popRequestAndThrowExceptionOnMessage(m, "Ping service failed"));
		dependentServiceStartup.getServerHandlerProvider().setHandler((m,s) -> respondWithPong(m));
		
		JsonMessage<JsonMessageContent> ping = new JsonMessage<>(JsonPingMessageContent.MESSAGE_NAME);
		ping.setSeq(1L);
		ping.setContent(new JsonPingMessageContent("Ping from client"));
		jsonWebSocketClientSessionPool.sendAsyncMessage(getServiceConnectionInformation(), ping);
		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		JsonMessage<JsonPongMessageContent> pong = (JsonMessage<JsonPongMessageContent>) testClientReceivedMessageCollector
				.getReceivedMessageList().get(0);
		assertNotNull(pong.getSeq());
		assertThat(pong.getName(), equalTo(JsonPongMessageContent.MESSAGE_NAME));
		assertThat(pong.getFirstAck(), equalTo(ping.getSeq()));
		assertNotNull(pong.getError());
		assertThat(pong.getError().getCode(), equalTo(ExceptionCodes.ERR_ENTRY_NOT_FOUND));		
		assertThat(pong.getError().getType(), equalTo(ExceptionType.CLIENT.name().toLowerCase()));
	}
	
	//XXX: similar functionality as described in these handlers is already implemented and used in multiple services. 
	//Consider, if this "know-how" should be made as a class/method.
	private JsonMessageContent onMessageRegisterInCacheAndForwardToDependentService(RequestContext context,
			SessionWrapper sessionWrapper) {
		JsonMessage<? extends JsonMessageContent> originalMessageDecoded = context.getMessage();
		LOGGER.info("Ping server received {}", originalMessageDecoded);
		JsonPingMessageContent originalContent = (JsonPingMessageContent) originalMessageDecoded.getContent();
		try {
			JsonWebSocketClientSessionPool mockServiceJsonWebSocketClientSessionPool =
					jettyServerRule.getServerStartup()
					.getInjector().getInstance(JsonWebSocketClientSessionPool.class);
			ServiceConnectionInformation dependencyServiceConnectionInformation = getDependencyServiceConnectionInformation();
			JsonMessage<JsonMessageContent> messageForForward = new JsonMessage<>(JsonPingMessageContent.MESSAGE_NAME);
			messageForForward.setSeq(2L);
			messageForForward.setContent(new JsonPingMessageContent("Forwarded info : " + originalContent.getPing()));
			
			RequestInfo requestInfo = new RequestInfoImpl(originalMessageDecoded);
			requestInfo.setSourceSession(sessionWrapper);
			mockServiceJsonWebSocketClientSessionPool.sendAsyncMessageWithClientInfo(dependencyServiceConnectionInformation,
					messageForForward,
					requestInfo);
		} catch (URISyntaxException e) {
			throw new UnexpectedTestException("Unexpected error"); //here ThrowingBiFunction would come in handy, so no catch is necessary
		}
		return null;
	}

	private JsonMessageContent onMessageForwardResponseToOriginalService(RequestContext context) {
		JsonMessage<? extends JsonMessageContent> originalMessageDecoded = context.getMessage();
		LOGGER.info("Ping client received {}", originalMessageDecoded);
		JsonPongMessageContent originalContent = (JsonPongMessageContent) originalMessageDecoded.getContent();
		RequestInfo originalRequest = context.getOriginalRequestInfo();
		JsonMessage<? extends JsonMessageContent> originalMessageFromClient = originalRequest.getSourceMessage();
		
		//quite similar with JsonMessageHandler.sendResponse just in different session. How delayed response is made in real services?
		final String messageResponseName = JsonMessage.getResponseMessageName(originalMessageDecoded.getName());
		JsonMessage<JsonMessageContent> responseMessage = jsonMessageUtil.createNewResponseMessage(messageResponseName,
				new JsonPongMessageContent("Forwarded response : " + originalContent.getPong()), originalMessageFromClient);
		originalRequest.getSourceSession().sendAsynMessage(responseMessage);
		return null;
	}

	private ServiceConnectionInformation getDependencyServiceConnectionInformation() throws URISyntaxException {
		F4MConfigImpl dependencyServiceConfig = mockProfileService.getServerStartup().getInjector().getInstance(F4MConfigImpl.class);
		return new ServiceConnectionInformation(dependencyServiceConfig.getProperty(F4MConfig.SERVICE_NAME), 
				((F4MConfigImpl)dependencyServiceConfig).getServiceURI().toString(), 
				dependencyServiceConfig.getPropertyAsListOfStrings(F4MConfig.SERVICE_NAMESPACES));
	}

	private JsonMessageContent popRequestAndThrowExceptionOnMessage(RequestContext context, String exceptionText) {
		JsonMessage<? extends JsonMessageContent> originalMessageDecoded = context.getMessage();
		RequestInfo originalRequest = context.getOriginalRequestInfo();
		LOGGER.info("Exception throwing client received {} and original from session was {}", originalMessageDecoded, originalRequest);
		throw new F4MEntryNotFoundException(exceptionText);
	}
	
	private JsonMessageContent throwExceptionOnMessage(RequestContext context, String exceptionText) {
		LOGGER.info("Exception throwing client received {}", context.getMessage());
		throw new F4MEntryNotFoundException(exceptionText);
	}
	
	private JsonMessageContent respondWithPong(RequestContext context) {
		JsonMessage<? extends JsonMessageContent> originalMessageDecoded = context.getMessage();
		LOGGER.info("Dependent service server received {}", originalMessageDecoded);
		JsonPingMessageContent originalContent = (JsonPingMessageContent) originalMessageDecoded.getContent();
		return new JsonPongMessageContent("Response from dependent service on '" + originalContent.getPing() + "'");
	}
}
