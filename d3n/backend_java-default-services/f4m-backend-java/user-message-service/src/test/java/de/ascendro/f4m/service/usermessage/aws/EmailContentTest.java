package de.ascendro.f4m.service.usermessage.aws;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;

import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.model.AppConfigBuilder;
import de.ascendro.f4m.service.exception.UnexpectedTestException;
import de.ascendro.f4m.service.json.model.ISOLanguage;
import de.ascendro.f4m.service.usermessage.ClientInfoBuilder;
import de.ascendro.f4m.service.usermessage.config.UserMessageConfig;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperResponse;
import de.ascendro.f4m.service.usermessage.translation.Messages;
import de.ascendro.f4m.service.usermessage.translation.TranslationPlaceholderReplacer;
import de.ascendro.f4m.service.usermessage.translation.Translator;
import de.ascendro.f4m.service.util.ProfileServiceTestHelper;

/**
 * Use VM argument emailContentsResultFolder to check contents of generated emails as html files, for example
 * -DemailContentsResultFolder=C:\Temp\emails\test_results
 */
public class EmailContentTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(EmailContentTest.class);

	@Rule
	public TestName testName = new TestName();
	@Mock
	private AmazonSimpleNotificationServiceWrapper snsWrapper;
	@Mock
	private CommonProfileAerospikeDao profileAerospikeDao;
	@Mock
	private ApplicationConfigurationAerospikeDao appConfigDao;

	private UserMessageConfig config = new UserMessageConfig();
	private Translator translator;
	private AmazonSimpleEmailServiceWrapper sesWrapper;
	private String email;
	private AmazonServiceCoordinator coordinator;

	private StringBuilder emailSubjectContents = new StringBuilder();
	private StringBuilder emailBodyContents = new StringBuilder();

	private SendEmailWrapperRequest testRequest;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		email = "mail1@mail1.com";
		translator = new Translator();
		prepareTestSesWrapper();
		coordinator = new AmazonServiceCoordinator(sesWrapper, snsWrapper, profileAerospikeDao, appConfigDao,
				translator, new TranslationPlaceholderReplacer(), config);
		when(profileAerospikeDao.getProfile(anyString())).thenReturn(ProfileServiceTestHelper.getTestProfile());
		when(appConfigDao.getAppConfiguration(anyString(), anyString())).thenReturn(AppConfigBuilder.buildDefaultAppConfig());
		prepareTestRequest();
	}

	private void prepareTestSesWrapper() {
		AmazonSimpleEmailServiceWrapper baseWrapper;
		//Set -DemailContentsResultFolder=... to see resulting emails as html
		String location = System.getProperty("emailContentsResultFolder");
		//Set -DawsAccessKey, -DawsSecretKey (and optionally -DawsRegion) to spam resulting emails to unlucky addess set by -Demail
		AWSCredentials credentials = config.getCredentials();
		if (location != null) {
			baseWrapper = new AmazonSimpleEmailServiceWrapper(null, null) {
				@Override
				public SendEmailWrapperResponse sendEmail(String toAddress, String subject, String messageText) {
					File outputFile = new File(location, testName.getMethodName() + ".html");
					try (OutputStream output = new FileOutputStream(outputFile)) {
						IOUtils.write(messageText, output, Charset.defaultCharset());
						LOGGER.warn("Please manually verify the looks of email content written to:\n{}", outputFile);
					} catch (IOException e) {
						throw new UnexpectedTestException("Could not write message to output", e);
					}
					return null;
				}
			};
		} else if (credentials != null) {
			AmazonSimpleEmailService sesClient = AwsSesExplorationTest.initSesClient(config);
			email = AwsSesExplorationTest.initEmail();
			baseWrapper = new AmazonSimpleEmailServiceWrapper(sesClient, config);
		} else {
			baseWrapper = mock(AmazonSimpleEmailServiceWrapper.class);
		}
		sesWrapper = new AmazonSimpleEmailServiceWrapper(null, null) {
			@Override
			public SendEmailWrapperResponse sendEmail(String toAddress, String subject, String messageText) {
				emailSubjectContents.append(subject);
				emailBodyContents.append(messageText);
				return baseWrapper.sendEmail(toAddress, subject, messageText);
			};
		};
	}
	
	private void prepareTestRequest() {
		testRequest = new SendEmailWrapperRequest();
		testRequest.setAddress(email);
		testRequest.setISOLanguage(ISOLanguage.EN);
	}
	
	private void withSubject(String subject, String... params) {
		testRequest.setSubject(subject);
		testRequest.setSubjectParameters(params);
	}

	private void withBody(String body, String... params) {
		testRequest.setMessage(body);
		testRequest.setParameters(params);
	}
	
	private void send() {
		testRequest.setUserId("userId");
		coordinator.sendEmail(testRequest, ClientInfoBuilder.fromTenantIdAppId("tenantId", "appId"));
	}
	
	private void sendToAdmin() {
		coordinator.sendEmail(testRequest, ClientInfoBuilder.fromTenantIdAppId("tenantId", "appId"));
	}
	
	private String[] getVoucherAssignedParams() {
		return new String[] {"Eat at McDonalds your whole life and get fat", "Quizz4Fitness",
				"AR7915T", "McDonalds", "2018-03-11",
				"http://www.redrobin.com/content/dam/web/menu/2015-june/gourmet-cheeseburger-1100.jpg", "http://www.google.com"};
	}
	
	private String[] getVoucherAssignedParamsNoRedemptionURL() {
		return new String[] {"Eat at McDonalds your whole life and get fat", "Quizz4Fitness",
				"AR7915T", "McDonalds", "2018-03-11",
				"http://www.redrobin.com/content/dam/web/menu/2015-june/gourmet-cheeseburger-1100.jpg"};
	}

	private String[] getTombolaPrizePayoutErrorEmailParams() {
		return new String[]{"ERROR_MESSAGE", "tombolaId", "transactionId", "1234", "MONEY", "tenantId", "userId",
				"voucherId"};
	}

	@Test
	public void voucherWonAssignedToUserWithRedemptionURL() throws Exception {
		withSubject(Messages.VOUCHER_WON_ASSIGNED_TO_USER_SUBJECT);
		withBody(Messages.VOUCHER_WON_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL, getVoucherAssignedParams());
		send();
		assertThat(emailSubjectContents.toString(), equalTo("The voucher you won is waiting for you."));
		assertThat(emailBodyContents.toString(), containsString(
				getEmailContent(true, true)));
	}

	@Test
	public void voucherWonAssignedToUser() throws Exception {
		withSubject(Messages.VOUCHER_WON_ASSIGNED_TO_USER_SUBJECT);
		withBody(Messages.VOUCHER_WON_ASSIGNED_TO_USER_CONTENT, getVoucherAssignedParamsNoRedemptionURL());
		send();
		assertThat(emailSubjectContents.toString(), equalTo("The voucher you won is waiting for you."));
		assertThat(emailBodyContents.toString(), containsString(getEmailContent(true, false)));
	}

	private String getEmailContent(boolean won, boolean hasRedemptionURL) {
		StringBuilder sb = new StringBuilder();
		if (won) {
			sb.append("Please find below the voucher Eat at McDonalds your whole life and get fat you won in the game Quizz4Fitness.");
		} else {
			sb.append("Please find below the voucher Eat at McDonalds your whole life and get fat you bought.");
		}
		sb.append(" You can redeem the voucher using the voucher code AR7915T at the store of McDonalds.");
		sb.append(" The voucher is valid and redeemable until its expiration date of 2018-03-11.<br/><br/>");
		if (hasRedemptionURL) {
			sb.append("<a href=\"http://www.google.com\">");
		}
		sb.append("<img src=\"http://www.redrobin.com/content/dam/web/menu/2015-june/gourmet-cheeseburger-1100.jpg\" alt=\"AR7915T\"/>");
		if (hasRedemptionURL) {
			sb.append("</a>");
		}
		sb.append("<br/><br/>Have lots of fun with this voucher and come back to play again.");
		return sb.toString();
	}

	@Test
	public void voucherWonInTombolaAssignedToUser() throws Exception {
		withSubject(Messages.VOUCHER_WON_ASSIGNED_TO_USER_SUBJECT);
		withBody(Messages.VOUCHER_WON_IN_TOMBOLA_ASSIGNED_TO_USER_CONTENT, getVoucherAssignedParamsNoRedemptionURL());
		send();
		assertThat(emailSubjectContents.toString(), equalTo("The voucher you won is waiting for you."));
		assertThat(emailBodyContents.toString(), containsString(
				"Please find below the voucher Eat at McDonalds your whole life and get fat you won in the tombola Quizz4Fitness."
						+ " You can redeem the voucher using the voucher code AR7915T at the store of McDonalds."
						+ " The voucher is valid and redeemable until its expiration date of 2018-03-11."
						+ "<br/><br/><img src=\"http://www.redrobin.com/content/dam/web/menu/2015-june/gourmet-cheeseburger-1100.jpg\""
						+ " alt=\"AR7915T\"/><br/><br/>Have lots of fun with this voucher and come back to play again."));
	}

	@Test
	public void voucherWonInTombolaAssignedToUserWithRedemptionURL() throws Exception {
		withSubject(Messages.VOUCHER_WON_ASSIGNED_TO_USER_SUBJECT);
		withBody(Messages.VOUCHER_WON_IN_TOMBOLA_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL, getVoucherAssignedParams());
		send();
		assertThat(emailSubjectContents.toString(), equalTo("The voucher you won is waiting for you."));
		assertThat(emailBodyContents.toString(), containsString(
				"Please find below the voucher Eat at McDonalds your whole life and get fat you won in the tombola Quizz4Fitness."
						+ " You can redeem the voucher using the voucher code AR7915T at the store of McDonalds."
						+ " The voucher is valid and redeemable until its expiration date of 2018-03-11."
						+ "<br/><br/><a href=\"http://www.google.com\">"
						+ "<img src=\"http://www.redrobin.com/content/dam/web/menu/2015-june/gourmet-cheeseburger-1100.jpg\""
						+ " alt=\"AR7915T\"/></a><br/><br/>Have lots of fun with this voucher and come back to play again."));
	}

	@Test
	public void tombolaPrizePayoutErrorToAdminEmail() throws Exception {
		withSubject(Messages.TOMBOLA_PRIZE_PAYOUT_ERROR_TO_ADMIN_SUBJECT);
		withBody(Messages.TOMBOLA_PRIZE_PAYOUT_ERROR_TO_ADMIN_CONTENT, getTombolaPrizePayoutErrorEmailParams());
		send();
		assertThat(emailSubjectContents.toString(), equalTo("An error occurred when assigning a tombola prize"));
		assertThat(emailBodyContents.toString(), containsString(
				"The following error occurred when assigning a tombola prize: ERROR_MESSAGE<br/>" +
						"Tombola and prize details:<br/>" +
						"Tombola ID: tombolaId<br/>" +
						"Transaction Log Id: transactionId<br/>" +
						"Amount: 1234<br/>" +
						"Currency: MONEY<br/>" +
						"Tenant ID: tenantId<br/>" +
						"User ID: userId<br/>" +
						"Voucher ID: voucherId<br/>"));
	}

	@Test
	public void voucherBoughtAssignedToUserWithRedemptionURL() throws Exception {
		withSubject(Messages.VOUCHER_BOUGHT_ASSIGNED_TO_USER_SUBJECT);
		withBody(Messages.VOUCHER_BOUGHT_ASSIGNED_TO_USER_CONTENT_WITH_REDEMPTION_URL, getVoucherAssignedParams());
		send();
		assertThat(emailSubjectContents.toString(), equalTo("The voucher you bought is waiting for you."));
		assertThat(emailBodyContents.toString(), containsString(getEmailContent(false, true)));
	}

	@Test
	public void voucherBoughtAssignedToUser() throws Exception {
		withSubject(Messages.VOUCHER_BOUGHT_ASSIGNED_TO_USER_SUBJECT);
		withBody(Messages.VOUCHER_BOUGHT_ASSIGNED_TO_USER_CONTENT, getVoucherAssignedParamsNoRedemptionURL());
		send();
		assertThat(emailSubjectContents.toString(), equalTo("The voucher you bought is waiting for you."));
		assertThat(emailBodyContents.toString(), containsString(getEmailContent(false, false)));
	}

	@Test
	public void authConfirmEmail() throws Exception {
		withSubject("auth.confirm.email.subject");
		withBody("auth.confirm.email.message");
		send();
		assertThat(emailSubjectContents.toString(), equalTo("Welcome to Quizz4Yournamehere! Your email is confirmed."));
		assertThat(emailBodyContents.toString(), containsString("Welcome to Quizz4Yournamehere! "
				+ "Your email " + email + " for player Name Surname has been confirmed. "
				+ "You are now a registered user within the app."));
	}

	@Test
	public void authConfirmEmailWithPassword() throws Exception {
		withSubject("auth.confirm.email.subject");
		withBody("auth.confirm.email.message.with.password", "pwd");
		send();
		assertThat(emailSubjectContents.toString(), equalTo("Welcome to Quizz4Yournamehere! Your email is confirmed."));
		assertThat(emailBodyContents.toString(), containsString("Welcome to Quizz4Yournamehere! "
				+ "Your email " + email + " for player Name Surname has been confirmed. "
				+ "You are now a registered user within the app. "
				+ "Please find here your temporary password pwd."));
	}

	@Test
	public void authChangePassword() throws Exception {
		withSubject("auth.change.password.subject");
		withBody("auth.change.password.message");
		send();
		assertThat(emailSubjectContents.toString(), equalTo("Your password has been changed successfully."));
		assertThat(emailBodyContents.toString(), containsString(
				"Your password change has been successfully processed and is now changed to your new entered password."
				+ " Please log in to the system with your new password."));
	}

	@Test
	public void authSetUserRole() throws Exception {
		withSubject("auth.set.user.role.subject");
		withBody("auth.set.user.role.message", "old_role", "new_role", "complete_role_definition");
		send();
		assertThat(emailSubjectContents.toString(), equalTo("You have a new role in the tenants game configurator Yournamehere"));
		assertThat(emailBodyContents.toString(), containsString("We have processed a role change for you."
				+ " Your old role old_role has been changed to the new role new_role."
				+ " With this new role you have the ability to do the following things in the tenants game configurator Yournamehere."
				+ "<br/><br/>Role Description: complete_role_definition"));
	}
	
	@Test
	public void authRegisterFacebook() throws Exception {
		withSubject("auth.register.facebook.subject");
		withBody("auth.register.facebook.message");
		send();
		assertThat(emailSubjectContents.toString(), equalTo("Welcome to the Quizz4Yournamehere! Your Facebook account has been confirmed."));
		assertThat(emailBodyContents.toString(), containsString("Welcome to the Quizz4Yournamehere!"
				+ " Your Facebook account for player Name Surname has been confirmed."
				+ " You are now a registered user within the app."));
	}

	@Test
	public void authRegisterGoogle() throws Exception {
		withSubject("auth.register.google.subject");
		withBody("auth.register.google.message");
		send();
		assertThat(emailSubjectContents.toString(), equalTo("Welcome to the Quizz4Yournamehere! Your Google account has been confirmed."));
		assertThat(emailBodyContents.toString(), containsString("Welcome to the Quizz4Yournamehere!"
				+ " Your Google account for player Name Surname has been confirmed."
				+ " You are now a registered user within the app."));
	}

	@Test
	public void authRecoverPasswordEmail() throws Exception {
		withSubject("auth.recover.password.email.subject");
		withBody("auth.recover.password.email.message", "pwd");
		send();
		assertThat(emailSubjectContents.toString(), equalTo("You or another person requested confirmation code to recover your email account."));
		assertThat(emailBodyContents.toString(), containsString(
				"In order to do that please verify your email and enter new password by using the following code:<br/><br/>Confirmation Code: pwd"));
	}

	@Test
	public void authInviteUserByEmail() throws Exception {
		withSubject("auth.invite.user.by.email.subject");
		withBody("auth.invite.user.by.email.message", "email@domain.com", "qwerty", "contents of user message", "Another User");
		send();
		assertThat(emailSubjectContents.toString(), equalTo("You have been invited to play quiz games hosted by Yournamehere."));
		assertThat(emailBodyContents.toString(), containsString(
				"You have been invited by Another User."
				+ " Another User would like to play quiz games against you or also see if you want to join tournaments."
				+ " Try out the super exciting game hosted by Yournamehere."
				+ "<br/><br/>Here is your friends personal message to you:"
				+ " contents of user message"
				+ "<br/><br/>The email address that was used is: email@domain.com"
				+ "<br/><br/>We have created a temporary password for you to quickly join: qwerty"
				//FIXME: the link is missing!
				+ "<br/><br/>Click on this link to open up the web-game or to get the app from app-store."
				));
	}
	
	@Test
	public void authInviteUserByEmailAndRole() throws Exception {
		withSubject("auth.invite.user.by.email.and.role.subject");
		withBody("auth.invite.user.by.email.and.role.message", "Translation administrator", "admin", "qwerty", "confirmCode",
				"http://apitools.dev4m.com/");
		send();
		assertThat(emailSubjectContents.toString(), equalTo("You have been invited to join the tenants game configurator Yournamehere."));
		assertThat(emailBodyContents.toString(), containsString(
				"You have been invited to join the tenants game configurator and question factory of Yournamehere."
				+ " Based on the role Translation administrator that was assigned to you, you have the ability to create and manage new games,"
				+ " design apps, or help in advancing the question world for the games played within the tenants Yournamehere game world."
				+ "<br/><br/>Here is your username: admin"
				+ "<br/>Here is your temporary password: qwerty"
				+ "<br/>Here is your confirmation code: confirmCode"
				+ "<br/><br/>Click on this <a href=\"http://apitools.dev4m.com/\">link</a> to get to the website and be able to log in."));
	}
	
	@Test
	public void authRegisterEmail() throws Exception {
		withSubject("auth.register.email.subject");
		withBody("auth.register.email.message", "confirmCode");
		send();
		assertThat(emailSubjectContents.toString(), equalTo("Welcome to the tenants game configurator Yournamehere."));
		assertThat(emailBodyContents.toString(), containsString("Welcome to the tenants game configurator Yournamehere."
				+ " In order to finish up your registration, please verify your email mail1@mail1.com by using the following code:"
				+ "<br/><br/>Confirmation Code: confirmCode"));
	}

	@Test
	public void authRegisterEmailNewcode() throws Exception {
		withSubject("auth.register.email.newcode.subject");
		withBody("auth.register.email.newcode.message", "confirmCode");
		send();
		assertThat(emailSubjectContents.toString(), equalTo("Welcome to the tenants game configurator Yournamehere."));
		assertThat(emailBodyContents.toString(), containsString(
				"You or another person requested a new confirmation code for your email mail1@mail1.com."
				+ " Please verify your email by using the following code:"
				+ "<br/><br/>Confirmation Code: confirmCode"));
	}

	@Test
	public void profileManagerProfileSyncCheck() throws Exception {
		withSubject("profileManager.profileSync.check.subject");
		withBody("profileManager.profileSync.check.message", "admin", "admin2", "birthDate");
		send();
		assertThat(emailSubjectContents.toString(), equalTo("Found inconsistency"));
		assertThat(emailBodyContents.toString(), containsString(
				"Found inconsistency for user admin merging to user admin2 following entry birthDate"));
	}

	@Test
	public void profileManagerProfileSyncGet() throws Exception {
		withSubject("profileManager.profileSync.get.subject");
		withBody("profileManager.profileSync.get.message", "admin");
		send();
		assertThat(emailSubjectContents.toString(), equalTo("Cannot request user"));
		assertThat(emailBodyContents.toString(), containsString(
				"Cannot request user admin data, validation fails"));
	}
	
	@Test
	public void voucherThresholdReaching() throws Exception {
		withSubject("voucher.threshold.reaching.subject", "Supervoucher");
		withBody("voucher.threshold.reaching.message", "Supervoucher");
		send();
		assertThat(emailSubjectContents.toString(), equalTo("Admin-Alert: Voucher Supervoucher threshold reached"));
		assertThat(emailBodyContents.toString(), containsString(
				"Please check the voucher Supervoucher. This voucher is about to run out of voucher codes."
				+ " Please invalidate the voucher or book new voucher codes to ensure the voucher can be bought or won within the assigned games."));
	}

	@Test
	public void gameInvalidDeactivated() throws Exception {
		withSubject("game.invalid.deactivated.subject", "Badgame");
		withBody("game.invalid.deactivated.message", "Badgame");
		send();
		assertThat(emailSubjectContents.toString(), equalTo("Admin-Alert: Game Badgame caused an error."));
		assertThat(emailBodyContents.toString(), containsString("Please check the game Badgame."
				+ " The system found errors in the game, for which the game could not be continued. The system has deactivated the game."
				+ " Please review and fix the game to ensure it is being entered back into game play mode."));
	}

	@Test
	public void hardcodedMessageWithoutTranslationToAdmin() throws Exception {
		final String subject = "Informative subject";
		final String body = "The message body";
		withSubject(subject);
		withBody(body);
		sendToAdmin();
		//check that email content and subject are sent as is
		assertThat(emailSubjectContents.toString(), equalTo(subject));
		assertThat(emailBodyContents.toString(), containsString(body));
		//but still maintaining translation in header/footer
		assertThat(emailBodyContents.toString(), containsString("Hello you"));
		assertThat(emailBodyContents.toString(), containsString("Enjoy your Game / Have fun from your<br/>Yournamehere - and friends4media Team"));
		
	}
}
