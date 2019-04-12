package de.ascendro.f4m.service.usermessage.aws;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.model.AppConfig;
import de.ascendro.f4m.server.profile.model.AppConfigTenant;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.json.model.user.ClientInfo;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.usermessage.config.UserMessageConfig;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperResponse;
import de.ascendro.f4m.service.usermessage.model.SendSmsRequest;
import de.ascendro.f4m.service.usermessage.model.SendSmsResponse;
import de.ascendro.f4m.service.usermessage.translation.Messages;
import de.ascendro.f4m.service.usermessage.translation.TranslatableMessage;
import de.ascendro.f4m.service.usermessage.translation.TranslationPlaceholderReplacer;
import de.ascendro.f4m.service.usermessage.translation.Translator;

public class AmazonServiceCoordinator {
	public static final String EMAIL_TEMPLATE_PATH = "/email_template.html";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AmazonServiceCoordinator.class);
	private AmazonSimpleEmailServiceWrapper sesWrapper;
	private AmazonSimpleNotificationServiceWrapper snsWrapper;
	private CommonProfileAerospikeDao profileAerospikeDao;
	private ApplicationConfigurationAerospikeDao appConfigDao;
	private Translator translator;
	private String emailTemplateContents;
	private TranslationPlaceholderReplacer placeholderReplacer;
	private Config config;

	@Inject
	public AmazonServiceCoordinator(AmazonSimpleEmailServiceWrapper sesWrapper,
			AmazonSimpleNotificationServiceWrapper snsWrapper, CommonProfileAerospikeDao profileAerospikeDao,
			ApplicationConfigurationAerospikeDao appConfigDao,
			Translator translator, TranslationPlaceholderReplacer placeholderReplacer,
			Config config) {
		this.sesWrapper = sesWrapper;
		this.snsWrapper = snsWrapper;
		this.profileAerospikeDao = profileAerospikeDao;
		this.appConfigDao = appConfigDao;
		this.translator = translator;
		this.placeholderReplacer = placeholderReplacer;
		this.config = config;
	}

	public JsonMessageContent sendSms(SendSmsRequest content, ClientInfo clientInfo) {
		LOGGER.debug("Received request to send an email {}", content);
		SendSmsResponse response;
		Profile profile = profileAerospikeDao.getProfile(content.getUserId());
		if (StringUtils.isBlank(content.getPhone()) || content.isLanguageAuto()) {
			if (content.isLanguageAuto()) {
				translator.updateLanguageFromProfile(profile, content);
			}
			if (StringUtils.isBlank(content.getPhone())) {
				String phone = profile.getLatestVerifiedPhone();
				if (phone == null) {
					phone = Iterables.getLast(profile.getProfilePhones());
				}
				content.setPhone(phone);
			}
		}
		
		if (StringUtils.isNotBlank(content.getPhone()) && ! content.isLanguageAuto()) {
			String smsText = translator.translate(content);
			smsText = placeholderReplacer.replaceProfilePlaceholders(smsText, profile, content);
			AppConfig appConfig = getAppConfiguration(clientInfo);
			smsText = placeholderReplacer.replaceAppDataPlaceholders(smsText, appConfig);
			smsText = placeholderReplacer.replaceTenantPlaceholders(smsText, appConfig);
			String phone = content.getPhone();
			response = snsWrapper.sendSMS(phone, smsText);
		} else {
			throw new F4MValidationFailedException("Phone number or language must be specified");
		}
		return response;
	}
	
	public SendEmailWrapperResponse sendEmail(SendEmailWrapperRequest content, ClientInfo clientInfo) {
		LOGGER.debug("Received request to send an email {}", content);
		SendEmailWrapperResponse response;
		Profile profile = profileAerospikeDao.getProfile(content.getUserId());
		if (StringUtils.isBlank(content.getAddress()) || content.isLanguageAuto()) {
			if (content.isLanguageAuto()) {
				translator.updateLanguageFromProfile(profile, content);
			}
			if (StringUtils.isBlank(content.getAddress())) {
				String mail = profile.getLatestVerifiedEmail();
				if (mail == null) {
					mail = Iterables.getLast(profile.getProfileEmails());
				}
				content.setAddress(mail);
			}
		}
		
		if (StringUtils.isNotBlank(content.getAddress()) && ! content.isLanguageAuto()) {
			AppConfig appConfig = getAppConfiguration(clientInfo);
			String emailSubject = prepareSubject(content, appConfig);
			String emailBody = prepareEmailBody(content, emailSubject, profile, appConfig);
			response = sesWrapper.sendEmail(content.getAddress(), emailSubject, emailBody);
		} else {
			throw new F4MValidationFailedException("Address and language must be specified");
		}
		return response;
	}

	private AppConfig getAppConfiguration(ClientInfo clientInfo) {
		AppConfig appConfig = null;
		if (notEmpty(clientInfo)) {
			appConfig = appConfigDao.getAppConfiguration(clientInfo.getTenantId(), clientInfo.getAppId());
		}
		return appConfig;
	}

	private boolean notEmpty(ClientInfo clientInfo) {
		return clientInfo != null && StringUtils.isNotEmpty(clientInfo.getTenantId());
	}
	
	private String prepareEmailBody(SendEmailWrapperRequest content, String emailSubject, Profile profile,
			AppConfig appConfig) {
		Optional<AppConfigTenant> tenant = Optional.ofNullable(appConfig).map(AppConfig::getTenant);
		String tenantName = placeholderReplacer.getTenantName(appConfig);
		String tenantUrl = tenant.map(AppConfigTenant::getUrl).map(StringUtils::stripToNull).orElse("#"); 
		String logoUrl = tenant.map(AppConfigTenant::getLogoUrl).map(StringUtils::stripToNull)
				.orElse(this.config.getProperty(UserMessageConfig.F4M_LOGO));
		String emailBody = readTemplate();
		
		//If more replaces come, consider using some templating lib like Velocity, Freemarker or similar
		emailBody = emailBody.replace("{{friendsForMedia.logourl}}", logoUrl);
		emailBody = emailBody.replace("{{subject}}", emailSubject);
		String name = null;
		if (profile != null && profile.getPersonWrapper() != null) {
			name = firstNotEmpty(profile.getPersonWrapper().getFirstName(), profile.getPersonWrapper().getNickname());
		}
		if (name == null) {
			name = prepareTranslatableMessage(Messages.EMAIL_TEMPLATE_HEADER_DEFAULT_ADDRESSEE, content);
		}
		emailBody = emailBody.replace("{{header}}", prepareTranslatableMessage(Messages.EMAIL_TEMPLATE_HEADER, content, name));
		emailBody = emailBody.replace("{{footer}}", prepareTranslatableMessage(Messages.EMAIL_TEMPLATE_FOOTER, content, tenantName));
		emailBody = emailBody.replace("{{friendsForMedia.url}}", tenantUrl);
		String translatedBody = translator.translate(content);
		translatedBody = translatedBody.replace("\n", "<br/>");
		emailBody = emailBody.replace("{{body}}", translatedBody);
		emailBody = placeholderReplacer.replaceAppDataPlaceholders(emailBody, appConfig);
		emailBody = placeholderReplacer.replaceProfilePlaceholders(emailBody, profile, content);
		emailBody = placeholderReplacer.replaceTenantPlaceholders(emailBody, appConfig);
		return emailBody;
	}
	
	private String prepareSubject(SendEmailWrapperRequest content, AppConfig appConfig) {
		String subject = prepareTranslatableMessage(content.getSubject(), content, content.getSubjectParameters());
		subject = placeholderReplacer.replaceAppDataPlaceholders(subject, appConfig);
		subject = placeholderReplacer.replaceTenantPlaceholders(subject, appConfig);
		return subject;
	}

	private String prepareTranslatableMessage(String message, TranslatableMessage baseMessage, String... parameters) {
		TranslatableMessage subjectMessage = new TranslatableMessage();
		subjectMessage.setMessage(message);
		subjectMessage.setISOLanguage(baseMessage.getISOLanguage());
		subjectMessage.setParameters(parameters);
		return translator.translate(subjectMessage);
	}
	
	private String readTemplate() {
		if (emailTemplateContents == null) {
			try (InputStream input = this.getClass().getResourceAsStream(EMAIL_TEMPLATE_PATH)) {
				emailTemplateContents = IOUtils.toString(input, "UTF-8");
			} catch (IOException e) {
				throw new F4MFatalErrorException("Could not read template", e);
			}
		}
		return emailTemplateContents;
	}

	private String firstNotEmpty(String... strings) {
		for (String string : strings) {
			if (StringUtils.isNotEmpty(string)) {
				return string;
			}
		}
		return null;
	}
}
