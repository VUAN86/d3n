package de.ascendro.f4m.service.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.exception.server.F4MIOException;
import de.ascendro.f4m.service.integration.test.F4MServiceIntegrationTestBase;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.ping.PingPongMessageSchemaMapper;
import de.ascendro.f4m.service.ping.PingPongMessageTypeMapper;
import de.ascendro.f4m.service.ping.PingPongServiceStartup;
import de.ascendro.f4m.service.ping.model.JsonPingMessageContent;
import de.ascendro.f4m.service.registry.model.ServiceConnectionInformation;
import de.ascendro.f4m.service.util.KeyStoreTestUtil;

/**
 * If you want to investigate SSL handshake in more details, add VM argument
 * <code>-Djavax.net.debug=ssl:handshake</code>
 */
public class CertificateValidationTest extends F4MServiceIntegrationTestBase {
	private static final String RUN_UNSTABLE_TESTS_PARAM_NAME = "runUnstableTests";
	private static final String EXPECTED_SEND_FAILURE_MESSAGE = "Has no WebSocket connection for URI[wss://localhost:8443/]";
	private static File testClientFile;
	private static File misconfiguredClientFile;
	private static File unknownClientFile;
	
	private PingPongServiceStartup pingPongServiceStartup;
	private Config clientConfig;
	private CountDownLatch pingPongCounterLock = new CountDownLatch(1);
	private ServiceConnectionInformation serviceConnInfo = new ServiceConnectionInformation("test",
			"wss://localhost:8443/", (String) null);
	
	@BeforeClass
	public static void setUpOtherKeystoreFiles() throws IOException {
		testClientFile = KeyStoreTestUtil.setupKeyStoreFile(keystoreFolder, "test_client.jks");
		misconfiguredClientFile = KeyStoreTestUtil.setupKeyStoreFile(keystoreFolder, "misconfigured_client.jks");
		unknownClientFile = KeyStoreTestUtil.setupKeyStoreFile(keystoreFolder, "unknown_client.jks");
		System.setProperty(F4MConfigImpl.JETTY_HANDSHAKE_REQUIRE_CLIENT_AUTH, Boolean.TRUE.toString());
	}
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		pingPongServiceStartup.getServerHandlerProvider().setHandler((m, s) -> countDownOnReceivedMessage());
		clientConfig = clientInjector.getInstance(Config.class);
	}

	private void assumeUnstableTestsShouldRun() {
		Assume.assumeTrue("Not running unstable tests, enable them using -D" + RUN_UNSTABLE_TESTS_PARAM_NAME + "=true",
				Boolean.valueOf(System.getProperty(RUN_UNSTABLE_TESTS_PARAM_NAME, Boolean.FALSE.toString())));
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		if (pingPongServiceStartup == null) {
			pingPongServiceStartup = new PingPongServiceStartup("PING_SERVICE");
		}
		return pingPongServiceStartup;
	}

	@Override
	protected TestClientInjectionClasses getTestClientInjectionClasses() {
		return new TestClientInjectionClasses(PingPongMessageTypeMapper.class, PingPongMessageSchemaMapper.class);
	}

	@Test
	public void pingWithSameCertificates() throws Exception {
		//theoretically covered by ServiceStartupPingPongTest.testPingPong(), but here is a different setup, 
		//so we need also a test with valid config to be sure setup of this test is working at all.
		jsonWebSocketClientSessionPool.sendAsyncMessage(serviceConnInfo, getPingMessage());
		pingPongCounterLock.await(2, TimeUnit.SECONDS);
	}

	@Test
	public void pingWithValidDifferentCertificates() throws Exception {
		clientConfig.setProperty(Config.KEY_STORE, testClientFile.getAbsolutePath());
		jsonWebSocketClientSessionPool.sendAsyncMessage(serviceConnInfo, getPingMessage());
		pingPongCounterLock.await(2, TimeUnit.SECONDS);
	}

	/**
	 * Timeout added because sometimes client freezes for eternity on handshake error.
	 * The exact reason is not clear, but it happens on de.ascendro.f4m.client.WebSocketClient.connect line
	 * <code>container.connectToServer</code>
	 * Possible reason is that org.eclipse.jetty.websocket.jsr356.ClientContainer.connect waits without timeout on
	 * <code>return (JsrSession)futSess.get();</code>
	 * where the PhysicalConnection is made.
	 */
	@Test(timeout=100_000)
	public void pingWithClientCertificateUnknownToServer() throws Exception {
		assumeUnstableTestsShouldRun(); //for some unknown reason, sometimes connection hangs instead of dropping. Seems to be an issue of Jetty.
		clientConfig.setProperty(Config.KEY_STORE, unknownClientFile.getAbsolutePath());
		sendPingWithExpectedFailure();
	}

	@Test(timeout=100_000)
	public void pingUnknownServerWithValidClientCertificate() throws Exception {
		clientConfig.setProperty(Config.KEY_STORE, misconfiguredClientFile.getAbsolutePath());
		sendPingWithExpectedFailure();
	}

	private void sendPingWithExpectedFailure() {
		try {
			jsonWebSocketClientSessionPool.sendAsyncMessage(serviceConnInfo, getPingMessage());
			fail("Message from unknown client should have failed");
		} catch (F4MIOException e) {
			assertEquals(EXPECTED_SEND_FAILURE_MESSAGE, e.getMessage());
		}
	}

	private JsonMessage<JsonPingMessageContent> getPingMessage() {
		final JsonMessage<JsonPingMessageContent> pingMessage = new JsonMessage<JsonPingMessageContent>(
				JsonPingMessageContent.MESSAGE_NAME);
		pingMessage.setContent(new JsonPingMessageContent("ping..."));
		return pingMessage;
	}

	private JsonMessageContent countDownOnReceivedMessage() {
		pingPongCounterLock.countDown();
		return null;
	}
}
