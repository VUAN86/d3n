package de.ascendro.f4m.service.usermessage.aws;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;

import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.model.AppConfigBuilder;
import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.usermessage.ClientInfoBuilder;
import de.ascendro.f4m.service.usermessage.config.UserMessageConfig;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.usermessage.translation.Messages;
import de.ascendro.f4m.service.usermessage.translation.TranslationPlaceholderReplacer;
import de.ascendro.f4m.service.usermessage.translation.Translator;
import de.ascendro.f4m.service.util.DateTimeUtil;
import de.ascendro.f4m.service.util.JsonLoader;
import de.ascendro.f4m.service.util.ProfileServiceTestHelper;

public class AwsSesExplorationTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(AwsSesExplorationTest.class);
	private UserMessageConfig config;
	private String email;
	private Translator translator;
	@Mock
	private AmazonSimpleNotificationServiceWrapper snsWrapper;
	@Mock
	private CommonProfileAerospikeDao profileAerospikeDao;
	@Mock
	private ApplicationConfigurationAerospikeDao appConfigDao;
	
	@Before
	public void setUp() {
		config = new UserMessageConfig();
		translator = new Translator();
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testEmailSendingWithAmazonServiceCoordinator() throws Exception {
		AWSCredentials credentials = config.getCredentials();
		assumeTrue(credentials != null); //ignore this test, if credentials are not set
		
		AmazonSimpleEmailService sesClient = initSesClient(config);
		email = initEmail();
		AmazonSimpleEmailServiceWrapper sesWrapper = new AmazonSimpleEmailServiceWrapper(sesClient, config);
		sendEmailFromAmazonServiceCoordinator(sesWrapper);
	}

	
	@Test
	public void testEmailSendingWithSESWrapper() throws Exception {
		AWSCredentials credentials = config.getCredentials();
		assumeTrue(credentials != null); //ignore this test, if credentials are not set
		
		AmazonSimpleEmailService sesClient = initSesClient(config);
		email = initEmail();
		AmazonSimpleEmailServiceWrapper sesWrapper = new AmazonSimpleEmailServiceWrapper(sesClient, config);
		String message = "This is a test message content using wrapped Amazon Simple Email Service on " 
				+ DateTimeUtil.getCurrentDateTime().toString()
				+ "<br/><br/> Regards, <br/>Your F4M team";
		message = JsonLoader.getTextFromResources("/email_template.html", this.getClass());
		sesWrapper.sendEmail(email, "Hello from F4M", message);
	}
	
	@Test
	public void testEmailSendingWithSES() throws Exception {
		AmazonSimpleEmailService sesClient = initSesClient(config);
		email = initEmail();
		
		Destination destination = new Destination().withToAddresses(email);
		Content subject = new Content("Subject of a message");
		Content text = new Content().withData("This is a test message content on " 
				+ DateTimeUtil.getCurrentDateTime()
				+ "<br/><br/> Regards, <br/>Your F4M team");
		Body body = new Body().withHtml(text);
		Message message = new Message().withSubject(subject).withBody(body);
		SendEmailRequest sendEmailRequest = new SendEmailRequest()
				.withSource(email)
				.withDestination(destination)
				.withMessage(message);
		SendEmailResult sendEmailResult = sesClient.sendEmail(sendEmailRequest);
		LOGGER.info("E-mail message was sent {}", sendEmailResult);
	}
	
	public static AmazonSimpleEmailService initSesClient(UserMessageConfig config) {
		//preconditions:
		// - AWS account accessKey and secretKey are set
		// - region set up
		// - email domain or address is verified in AWS (needed while we are in the sandbox)
		
		AWSCredentials credentials = config.getCredentials();
		assumeTrue("Skipping a test with real AWS SES, because no credentials are specified", credentials != null);
		
		AmazonSimpleEmailService sesClient = new AmazonSimpleEmailServiceClient(credentials);
		assertNotNull("Region must be specified", config.getRegion());
		sesClient.setRegion(config.getRegion());
		return sesClient;
	}

	public static String initEmail() {
		String email = System.getProperty("email");
		assertTrue("Source/destination email address not specified in parameter '-Demail'",
				StringUtils.isNotBlank(email));
		return email;
	}
	
	private void sendEmailFromAmazonServiceCoordinator(AmazonSimpleEmailServiceWrapper sesWrapper) {
		when(profileAerospikeDao.getProfile(any())).thenReturn(ProfileServiceTestHelper.getTestProfile());
		when(appConfigDao.getAppConfiguration(anyString(), anyString())).thenReturn(AppConfigBuilder.buildDefaultAppConfig());
		
		AmazonServiceCoordinator coordinator = new AmazonServiceCoordinator(sesWrapper, snsWrapper, profileAerospikeDao,
				appConfigDao, translator, new TranslationPlaceholderReplacer(), config);
		
		SendEmailWrapperRequest request = new SendEmailWrapperRequest();
		request.setAddress(email);
		request.setSubject(Messages.VOUCHER_WON_ASSIGNED_TO_USER_SUBJECT);
		request.setMessage(Messages.VOUCHER_WON_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL);
		request.setParameters(new String[] { "Eat at McDonalds your whole life and get fat", "Quizz4Fitness",
				"--==SupadupaVoucherCode==--", "McDonalds", "doomsday",
				"http://www.redrobin.com/content/dam/web/menu/2015-june/gourmet-cheeseburger-1100.jpg", "http://www.google.com" });
		request.setISOLanguage(ISOLanguage.EN);
		coordinator.sendEmail(request, ClientInfoBuilder.fromTenantIdAppId("tenantId", "appId"));
	}
}
