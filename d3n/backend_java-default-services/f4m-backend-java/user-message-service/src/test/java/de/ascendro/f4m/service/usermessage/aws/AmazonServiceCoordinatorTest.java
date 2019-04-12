package de.ascendro.f4m.service.usermessage.aws;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.model.AppConfig;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.usermessage.config.UserMessageConfig;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.usermessage.model.SendSmsRequest;
import de.ascendro.f4m.service.usermessage.translation.TranslatableMessage;
import de.ascendro.f4m.service.usermessage.translation.TranslationPlaceholderReplacer;
import de.ascendro.f4m.service.usermessage.translation.Translator;

public class AmazonServiceCoordinatorTest {
	@Mock
	private CommonProfileAerospikeDao profileAerospikeDao;
	@Mock
	private ApplicationConfigurationAerospikeDao appConfigDao;
	@Spy
	private Translator translator;
	@Spy
	private TranslationPlaceholderReplacer placeholderReplacer;
	@Spy
	private UserMessageConfig config;
	@Mock
	private AmazonSimpleEmailServiceWrapper sesWrapper;
	@Mock
	private AmazonSimpleNotificationServiceWrapper snsWrapper;
	@InjectMocks
	private AmazonServiceCoordinator amazonServiceCoordinator;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		//see de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDaoImpl.getAppConfigurationAsString
		when(appConfigDao.getAppConfiguration(null, null)).thenThrow(new F4MEntryNotFoundException("AppConfig not found"));
	}

	@Test
	public void sendEmailWithOnlyEmail() {
		SendEmailWrapperRequest content = createEmailContent();
		ClientInfo clientInfo = new ClientInfo();
		clientInfo.setTenantId("tenantId");
		when(appConfigDao.getAppConfiguration("tenantId", null)).thenReturn(new AppConfig());
		amazonServiceCoordinator.sendEmail(content, clientInfo);
		verifyEmailSent();
	}

	@Test
	public void sendEmailWithEmptyClientInfo() {
		SendEmailWrapperRequest content = createEmailContent();
		ClientInfo clientInfo = new ClientInfo();
		amazonServiceCoordinator.sendEmail(content, clientInfo);
		verifyEmailSent();
	}

	@Test
	public void sendEmailWithNullClientInfo() {
		SendEmailWrapperRequest content = createEmailContent();
		ClientInfo clientInfo = null;
		amazonServiceCoordinator.sendEmail(content, clientInfo);
		verifyEmailSent();
	}

	@Test
	public void sendSmsWithNullClientInfo() {
		SendSmsRequest content = new SendSmsRequest();
		content.setPhone("123456");
		fillTranslatableMessageParams(content);
		ClientInfo clientInfo = null;
		amazonServiceCoordinator.sendSms(content, clientInfo);
		verify(snsWrapper).sendSMS("123456", "specific message");
	}

	@Test
	public void sendSmsWithEmptyClientInfo() {
		SendSmsRequest content = new SendSmsRequest();
		content.setPhone("123456");
		fillTranslatableMessageParams(content);
		ClientInfo clientInfo = new ClientInfo();
		amazonServiceCoordinator.sendSms(content, clientInfo);
		verify(snsWrapper).sendSMS("123456", "specific message");
	}

	private void verifyEmailSent() {
		verify(sesWrapper).sendEmail(eq("email@ddress"), eq("subject"), contains("specific message"));
	}

	private SendEmailWrapperRequest createEmailContent() {
		SendEmailWrapperRequest content = new SendEmailWrapperRequest();
		content.setUserId("userId");
		content.setAddress("email@ddress");
		content.setSubject("subject");
		fillTranslatableMessageParams(content);
		return content;
	}

	private void fillTranslatableMessageParams(TranslatableMessage content) {
		content.setMessage("specific message");
		content.setISOLanguage(ISOLanguage.EN);
	}

}
