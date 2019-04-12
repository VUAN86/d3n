package de.ascendro.f4m.service.usermessage.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.MailFromDomainNotVerifiedException;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.exception.ExceptionCodes;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;

/**
 * Tests error message handling.
 */
public class ErrorMessageForwardingTest extends UserMessageServiceApiTestBase {
	private AmazonSimpleEmailService amazonSimpleEmailServiceClient;
	
	@Override
	public void setUp() throws Exception {
		super.setUp();
		amazonSimpleEmailServiceClient = jettyServerRule.getServerStartup().getInjector()
				.getInstance(AmazonSimpleEmailService.class);
	}
	
	@Test
	public void testEmailSendingWithError() throws Exception {
		when(amazonSimpleEmailServiceClient.sendEmail(any()))
				.thenThrow(new MailFromDomainNotVerifiedException( "Could not send e-mail as expected"));
		String request = jsonLoader.getPlainTextJsonFromResources(AmazonMessageApiTest.AMAZON_TEST_JSON_LOCATION + "sendEmailRequest.json");
		JsonMessage<? extends JsonMessageContent> jsonContent = jsonUtil.fromJson(request);
		jsonWebSocketClientSessionPool.sendAsyncMessage(getServiceConnectionInformation(), jsonContent);
		RetriedAssert.assertWithWait(
				() -> assertEquals(1, testClientReceivedMessageCollector.getReceivedMessageList().size()));
		JsonMessage<? extends JsonMessageContent> errorResponse = testClientReceivedMessageCollector
				.getReceivedMessageList().get(0);
		assertNotNull("Unexpected error returned", errorResponse.getError());
		assertEquals(errorResponse.getError().getCode(),  ExceptionCodes.ERR_FATAL_ERROR);
		assertEquals(1, errorResponse.getAck().length);
		assertEquals(jsonContent.getSeq(), (Long) errorResponse.getAck()[0]);
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return AmazonMessageApiTest.createTestUserMessageServiceStartup();
	}
}
