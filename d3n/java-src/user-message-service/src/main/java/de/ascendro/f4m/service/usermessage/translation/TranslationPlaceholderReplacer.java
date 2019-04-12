package de.ascendro.f4m.service.usermessage.translation;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import de.ascendro.f4m.server.profile.model.AppConfig;
import de.ascendro.f4m.server.profile.model.AppConfigApplication;
import de.ascendro.f4m.server.profile.model.AppConfigTenant;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.usermessage.model.SendEmailWrapperRequest;
import de.ascendro.f4m.service.usermessage.model.SendSmsRequest;

public class TranslationPlaceholderReplacer {

	public String replaceAppDataPlaceholders(String message, AppConfig appConfig) {
		String result = message;
		Optional<AppConfigApplication> app = Optional.ofNullable(appConfig).map(AppConfig::getApplication);
		String appName = app.map(AppConfigApplication::getTitle).map(StringUtils::stripToNull).orElse("app");
		result = result.replace("[application.title]", appName);
		return result;
	}
	
	public String replaceProfilePlaceholders(String message, Profile profile, SendEmailWrapperRequest content) {
		String result = message.replace("[profile.email]", content.getAddress());
		return replaceProfilePlaceholders(result, profile);
	}

	public String replaceProfilePlaceholders(String message, Profile profile, SendSmsRequest content) {
		String result = message.replace("[profile.phone]", content.getPhone());
		return replaceProfilePlaceholders(result, profile);
	}

	public String replaceProfilePlaceholders(String message, Profile profile) {
		String result = message;
		
		final String firstName;
		final String lastName;
		if (profile != null && profile.getPersonWrapper() != null) {
			firstName = profile.getPersonWrapper().getFirstName();
			lastName = profile.getPersonWrapper().getLastName();
		} else {
			firstName = "";
			lastName = "";
		}
		result = result.replace("[profile.firstName]", firstNonNull(firstName, ""));
		result = result.replace("[profile.lastName]", firstNonNull(lastName, ""));
		return result;
	}
	
	public String replaceTenantPlaceholders(String message, AppConfig appConfig) {
		String result = message;
		result = result.replace("[tenant.name]", getTenantName(appConfig));
		return result;
	}
	
	public String getTenantName(AppConfig appConfig) {
		Optional<AppConfigTenant> tenant = Optional.ofNullable(appConfig).map(AppConfig::getTenant);
		return tenant.map(AppConfigTenant::getName).map(StringUtils::stripToNull).orElse("Tenant");
	}
}
