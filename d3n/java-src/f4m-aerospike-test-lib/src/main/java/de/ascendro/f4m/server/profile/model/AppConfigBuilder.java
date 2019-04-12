package de.ascendro.f4m.server.profile.model;

import java.math.BigDecimal;

public class AppConfigBuilder {

	private AppConfig appConfig;
	
	public AppConfigBuilder() {
		appConfig = new AppConfig();
	}
	
	public AppConfigBuilder withAppTitle(String appTitle) {
		initApplication();
		appConfig.getApplication().setTitle(appTitle);
		return this;
	}
	
	public AppConfigBuilder withCdnMedia(String cdnMedia) {
		initApplicationConfiguration();
		appConfig.getApplication().getConfiguration().setCdnMedia(cdnMedia);
		return this;
	}

	public AppConfigBuilder withTenantName(String tenantName) {
		initTenant();
		appConfig.getTenant().setName(tenantName);
		return this;
	}

	public AppConfigBuilder withTenantUrl(String url) {
		initTenant();
		appConfig.getTenant().setUrl(url);
		return this;
	}

	public AppConfigBuilder withLogoUrl(String logoUrl) {
		initTenant();
		appConfig.getTenant().setLogoUrl(logoUrl);
		return this;
	}
	
	public AppConfig build() {
		return appConfig;
	}

	private void initTenant() {
		if (appConfig.getTenant() == null) {
			appConfig.setTenant(new AppConfigTenant());
		}
	}
	
	private void initApplication() {
		if (appConfig.getApplication() == null) {
			appConfig.setApplication(new AppConfigApplication());
		}
	}
	
	private void initApplicationConfiguration() {
		initApplication();
		if (appConfig.getApplication().getConfiguration() == null) {
			appConfig.getApplication().setConfiguration(new AppConfigApplicationConfiguration());
		}
	}
	
	public static AppConfig buildDefaultAppConfig() {
		return new AppConfigBuilder()
				.withCdnMedia("http://f4m-nightly-medias.s3.amazonaws.com/")
				.withAppTitle("Quizz4Yournamehere")
				.withTenantName("Yournamehere")
				.withTenantUrl("http://www.f4m.tv").build();
	}

	public static AppConfig buildDefaultAppConfigWithAdvertisement() {
		AppConfig appConfig = AppConfigBuilder.buildDefaultAppConfig();
		AppConfigApplicationConfiguration appConfigApplicationConfiguration = new AppConfigApplicationConfiguration();
		appConfigApplicationConfiguration.setNumberOfPreloadedAdvertisements(5);
		appConfig.getApplication().setConfiguration(appConfigApplicationConfiguration);
		return appConfig;
	}

	public static AppConfig buildDefaultAppConfigWithBonusPayouts(BigDecimal bonuspointsForDailyLogin,
			BigDecimal bonuspointsForRating, BigDecimal bonuspointsForSharing) {
		AppConfig appConfig = AppConfigBuilder.buildDefaultAppConfig();
		appConfig.getApplication().getConfiguration().setBonuspointsForDailyLogin(bonuspointsForDailyLogin);
		appConfig.getApplication().getConfiguration().setBonuspointsForRating(bonuspointsForRating);
		appConfig.getApplication().getConfiguration().setBonuspointsForSharing(bonuspointsForSharing);
		return appConfig;
	}

	public static AppConfig buildDefaultAppConfigWithRegitrationBounus(BigDecimal fullRegistrationCredits,
		   BigDecimal fullRegistrationBonusPoints, BigDecimal firstTimeSignUpCredits,BigDecimal firstTimeSignUpBonusPoints) {
		AppConfig appConfig = AppConfigBuilder.buildDefaultAppConfig();
		appConfig.getApplication().getConfiguration().setFullRegistrationCredits(fullRegistrationCredits);
		appConfig.getApplication().getConfiguration().setFullRegistrationBonusPoints(fullRegistrationBonusPoints);
		appConfig.getApplication().getConfiguration().setSimpleRegistrationCredits(firstTimeSignUpCredits);
		appConfig.getApplication().getConfiguration().setSimpleRegistrationBonusPoints(firstTimeSignUpBonusPoints);
		return appConfig;
	}
}
