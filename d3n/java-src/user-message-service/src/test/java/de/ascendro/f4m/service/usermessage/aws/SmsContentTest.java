package de.ascendro.f4m.service.usermessage.aws;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.model.AppConfigBuilder;
import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.usermessage.ClientInfoBuilder;
import de.ascendro.f4m.service.usermessage.model.SendSmsRequest;
import de.ascendro.f4m.service.usermessage.translation.TranslationPlaceholderReplacer;
import de.ascendro.f4m.service.usermessage.translation.Translator;
import de.ascendro.f4m.service.util.ProfileServiceTestHelper;

public class SmsContentTest {
	private static final String TEST_TENANT_ID = "tenantId";
	private static final String TEST_APP_ID = "appId";
	@Mock
	private AmazonSimpleNotificationServiceWrapper snsWrapper;
	@Mock
	private CommonProfileAerospikeDao profileAerospikeDao;
	@Mock
	private ApplicationConfigurationAerospikeDao appConfigDao;
	@Spy
	private TranslationPlaceholderReplacer placeholderReplacer;
	@Spy
	private Translator translator;
	@InjectMocks
	private AmazonServiceCoordinator coordinator;
	private SendSmsRequest testRequest;
	private ArgumentCaptor<String> smsArg;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		when(profileAerospikeDao.getProfile(any())).thenReturn(ProfileServiceTestHelper.getTestProfile());
		when(appConfigDao.getAppConfiguration(TEST_TENANT_ID, TEST_APP_ID)).thenReturn(AppConfigBuilder.buildDefaultAppConfig());
		prepareTestRequest();
		smsArg = ArgumentCaptor.forClass(String.class);
	}
	
	private void prepareTestRequest() {
		testRequest = new SendSmsRequest();
		testRequest.setPhone("321321");
		testRequest.setISOLanguage(ISOLanguage.EN);
	}
	
	private void withSms(String body, String... params) {
		testRequest.setMessage(body);
		testRequest.setParameters(params);
	}
	
	private void send() {
		coordinator.sendSms(testRequest, ClientInfoBuilder.fromTenantIdAppId(TEST_TENANT_ID, TEST_APP_ID));
		verify(snsWrapper).sendSMS(eq("321321"), smsArg.capture());
	}

	@Test
	public void authConfirmPhoneSms() throws Exception {
		withSms("auth.confirm.phone.sms");
		send();
		assertThat(smsArg.getValue(), equalTo("Welcome to Quizz4Yournamehere!"
				+ " Your phone number 321321 for player Name Surname has been confirmed. You are now a registered user within the app."));
	}

	@Test
	public void authConfirmPhoneSmsWithPassword() throws Exception {
		withSms("auth.confirm.phone.sms.with.password", "pwdHere");
		send();
		assertThat(smsArg.getValue(), equalTo("Welcome to Quizz4Yournamehere!"
				+ " Your phone number 321321 for player Name Surname has been confirmed. You are now a registered user within the app."
				+ " Please find here your temporary password pwdHere."));
	}
	
	@Test
	public void authRecoverPasswordPhoneSms() throws Exception {
		withSms("auth.recover.password.phone.sms", "pwdHere");
		send();
		assertThat(smsArg.getValue(), equalTo("You or another person requested confirmation code to recover your phone."
				+ " In order to do that please verify your phone and enter new password by using the following code: pwdHere."));
	}
	
	@Test
	public void authRegisterPhoneSms() throws Exception {
		withSms("auth.register.phone.sms", "codeHere");
		send();
		assertThat(smsArg.getValue(), equalTo("Welcome to the tenants game configurator Yournamehere."
				+ " In order to finish up your registration, please verify your phone 321321 by using the following code: codeHere"));
	}
	
	@Test
	public void authRegisterPhoneNewcodeSms() throws Exception {
		withSms("auth.register.phone.newcode.sms", "codeHere");
		send();
		assertThat(smsArg.getValue(), equalTo("You or another person requested a new confirmation code for your phone 321321."
				+ " Please verify your phone by using the following code: codeHere"));
	}
}
