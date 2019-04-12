package de.ascendro.f4m.service.usermessage.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishResult;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.usermessage.UserMessageServiceStartup;

public class AmazonMessageApiTest extends UserMessageServiceApiTestBase {
	
	static final String AMAZON_TEST_JSON_LOCATION = "amazon/";
	private AmazonSimpleEmailService amazonSimpleEmailServiceClient;
	private AmazonSNS amazonSNS;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		Injector serverInjector = jettyServerRule.getServerStartup().getInjector();
		amazonSimpleEmailServiceClient = serverInjector.getInstance(AmazonSimpleEmailService.class);
		amazonSNS = serverInjector.getInstance(AmazonSNS.class);
	}

	@Override
	protected ServiceStartup getServiceStartup() {
		return createTestUserMessageServiceStartup();
	}
	
	public static UserMessageServiceStartup createTestUserMessageServiceStartup() {
		return new UserMessageServiceStartup(DEFAULT_TEST_STAGE) {
			@Override
			protected Module getModule() {
				return Modules.override(super.getModule()).with(new UserMessageMockModule());
			}
		};
	}

	@Test
	public void testEmailSendingToSpecifiedEmail() throws Exception {
		when(amazonSimpleEmailServiceClient.sendEmail(any()))
				.thenReturn(new SendEmailResult().withMessageId("mockedMessageId"));
		verifyResponseOnRequest("sendEmailRequest.json", "sendEmailResponseExpected.json");
		verify(amazonSimpleEmailServiceClient).sendEmail(argThat(
				request -> request.getDestination().getToAddresses().contains("test@email.com")));
	}
	
	@Test
	public void testSmsSending() throws Exception {
		when(amazonSNS.publish(any()))
				.thenReturn(new PublishResult().withMessageId("mockedSmsMessageId"));
		verifyResponseOnRequest("sendSmsRequest.json", "sendSmsResponseExpected.json");
		verify(amazonSNS).publish(argThat(request -> "not_a_real_phone_number".equals(request.getPhoneNumber())
				&& "test message content".equals(request.getMessage())));
	}
	
	@Test
	public void testEmailSendingToUserId() throws Exception {
		when(amazonSimpleEmailServiceClient.sendEmail(any()))
				.thenReturn(new SendEmailResult().withMessageId("mockedMessageId"));
		verifyResponseOnRequest("sendEmailByUserIdRequest.json", "sendEmailByUserIdResponseExpected.json");
		verify(amazonSimpleEmailServiceClient).sendEmail(argThat(
				request -> request.getDestination().getToAddresses().contains("mail1@mail1.com")));
	}

	@Test
	public void testEmailSendingWithTranslations() throws Exception {
		when(amazonSimpleEmailServiceClient.sendEmail(any()))
				.thenReturn(new SendEmailResult().withMessageId("mockedMessageId"));
		verifyResponseOnRequest("sendEmailWithTranslationRequest.json", "sendEmailResponseExpected.json");
		verify(amazonSimpleEmailServiceClient).sendEmail(argThat(
				request -> request.getDestination().getToAddresses().contains("test@gmail.com")));
		verify(amazonSimpleEmailServiceClient)
				.sendEmail(argThat(request -> request.getMessage().getBody().getHtml().getData().contains(
						"You have been assigned to work in and on our category CategoryContents.")));
		verify(amazonSimpleEmailServiceClient).sendEmail(argThat(request -> request.getMessage().getSubject().getData()
				.equals("You have been assigned to work on our category CategorySubject.")));
	}
	
	@Test
	public void testSmsSendingToUserId() throws Exception {
		when(amazonSNS.publish(any())).thenReturn(new PublishResult().withMessageId("mockedSmsMessageId"));
		verifyResponseOnRequest("sendSmsByUserIdRequest.json", "sendSmsResponseExpected.json");
		verify(amazonSNS).publish(argThat(request -> "255689987".equals(request.getPhoneNumber())
				&& "test message content".equals(request.getMessage())));
	}
	
	@Override
	public void verifyResponseOnRequest(String requestPath, String expectedResponsePath) throws Exception {
		super.verifyResponseOnRequest(AMAZON_TEST_JSON_LOCATION + requestPath, AMAZON_TEST_JSON_LOCATION + expectedResponsePath);
	}
}
