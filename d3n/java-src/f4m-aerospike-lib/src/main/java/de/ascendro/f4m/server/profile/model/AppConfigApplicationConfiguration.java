package de.ascendro.f4m.server.profile.model;

import java.math.BigDecimal;

/**
 * Partial model of AppConfiguration.application.configuration object returned by getAppConfiguration (not all attributes)
 */
public class AppConfigApplicationConfiguration {
	private String cdnMedia;
	private BigDecimal bonuspointsForSharing;
	private BigDecimal bonuspointsForRating;
	private BigDecimal bonuspointsForDailyLogin;
	private AppConfigFyber fyber;
	private String defaultAdvertisementImageId;
	private int numberOfPreloadedAdvertisements;
	private long advertisementProviderId;
	private BigDecimal simpleRegistrationBonusPoints;
	private BigDecimal simpleRegistrationCredits;
	private BigDecimal fullRegistrationBonusPoints;
	private BigDecimal fullRegistrationCredits;
	private String downloadUrl;

	public AppConfigFyber getFyber() {
		return fyber;
	}

	public void setFyber(AppConfigFyber fyber) {
		this.fyber = fyber;
	}

	public String getCdnMedia() {
		return cdnMedia;
	}

	public void setCdnMedia(String cdnMedia) {
		this.cdnMedia = cdnMedia;
	}

	public BigDecimal getBonuspointsForSharing() {
		return bonuspointsForSharing;
	}

	public void setBonuspointsForSharing(BigDecimal bonuspointsForSharing) {
		this.bonuspointsForSharing = bonuspointsForSharing;
	}

	public BigDecimal getBonuspointsForRating() {
		return bonuspointsForRating;
	}

	public void setBonuspointsForRating(BigDecimal bonuspointsForRating) {
		this.bonuspointsForRating = bonuspointsForRating;
	}

	public BigDecimal getBonuspointsForDailyLogin() {
		return bonuspointsForDailyLogin;
	}

	public void setBonuspointsForDailyLogin(BigDecimal bonuspointsForDailyLogin) {
		this.bonuspointsForDailyLogin = bonuspointsForDailyLogin;
	}

	public String getDefaultAdvertisementImageId() {
		return defaultAdvertisementImageId;
	}

	public void setDefaultAdvertisementImageId(String defaultAdvertisementImageId) {
		this.defaultAdvertisementImageId = defaultAdvertisementImageId;
	}

	public int getNumberOfPreloadedAdvertisements() {
		return numberOfPreloadedAdvertisements;
	}

	public void setNumberOfPreloadedAdvertisements(int numberOfPreloadedAdvertisements) {
		this.numberOfPreloadedAdvertisements = numberOfPreloadedAdvertisements;
	}

	public long getAdvertisementProviderId() {
		return advertisementProviderId;
	}

	public void setAdvertisementProviderId(long advertisementProviderId) {
		this.advertisementProviderId = advertisementProviderId;
	}

    public BigDecimal getSimpleRegistrationBonusPoints() {
        return simpleRegistrationBonusPoints;
    }

    public void setSimpleRegistrationBonusPoints(BigDecimal simpleRegistrationBonusPoints) {
        this.simpleRegistrationBonusPoints = simpleRegistrationBonusPoints;
    }

    public BigDecimal getSimpleRegistrationCredits() {
        return simpleRegistrationCredits;
    }

    public void setSimpleRegistrationCredits(BigDecimal simpleRegistrationCredits) {
        this.simpleRegistrationCredits = simpleRegistrationCredits;
    }

    public BigDecimal getFullRegistrationBonusPoints() {
        return fullRegistrationBonusPoints;
    }

    public void setFullRegistrationBonusPoints(BigDecimal fullRegistrationBonusPoints) {
        this.fullRegistrationBonusPoints = fullRegistrationBonusPoints;
    }

    public BigDecimal getFullRegistrationCredits() {
        return fullRegistrationCredits;
    }

    public void setFullRegistrationCredits(BigDecimal fullRegistrationCredits) {
        this.fullRegistrationCredits = fullRegistrationCredits;
    }

    public static boolean isValueSet(BigDecimal value) {
		return  (value!=null && value.signum()>0);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("AppConfigApplicationConfiguration{");
		sb.append("cdnMedia='").append(cdnMedia).append('\'');
		sb.append(", bonuspointsForSharing=").append(bonuspointsForSharing);
		sb.append(", bonuspointsForRating=").append(bonuspointsForRating);
		sb.append(", bonuspointsForDailyLogin=").append(bonuspointsForDailyLogin);
		sb.append(", defaultAdvertisementImageId='").append(defaultAdvertisementImageId).append('\'');
		sb.append(", numberOfPreloadedAdvertisements=").append(numberOfPreloadedAdvertisements);
		sb.append(", advertisementProviderId=").append(advertisementProviderId);
		sb.append(", simpleRegistrationBonusPoints=").append(simpleRegistrationBonusPoints);
		sb.append(", simpleRegistrationCredits=").append(simpleRegistrationCredits);
		sb.append(", fullRegistrationBonusPoints=").append(fullRegistrationBonusPoints);
		sb.append(", fullRegistrationCredits=").append(fullRegistrationCredits);
		sb.append(", fyber=").append(fyber);
		sb.append(", downloadUrl=").append(downloadUrl);
		sb.append('}');
		return sb.toString();
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}
}
