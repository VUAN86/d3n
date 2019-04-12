package de.ascendro.f4m.service.json.model.user;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.stream.Stream;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.annotations.SerializedName;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.json.model.ISOCountry;
import de.ascendro.f4m.service.json.model.JsonMessage;

public class ClientInfo {
	public static final String TENANT_ROLE_PREFIX = "TENANT_";
	public static final String TENANT_ROLE_SEPARATOR = "_";

	public static final String MESSAGE_CLIENT_INFO_PROFILE_PROPERTY = "profile";
	public static final String MESSAGE_CLIENT_INFO_PROFILE_USER_ID_PROPERTY = "userId";
	public static final String MESSAGE_CLIENT_INFO_PROFILE_LANGUAGE_PROPERTY = "language";
	public static final String MESSAGE_CLIENT_INFO_PROFILE_HANDICAP_PROPERTY = "handicap";
	public static final String MESSAGE_CLIENT_INFO_PROFILE_ROLES_PROPERTY = "roles";
	public static final String MESSAGE_CLIENT_INFO_PROFILE_EMAILS_PROPERTY = "emails";
	public static final String MESSAGE_CLIENT_INFO_PROFILE_PHONES_PROPERTY = "phones";
	
	public static final String MESSAGE_CLIENT_INFO_APP_CONFIG_PROPERTY = "appConfig";
	public static final String MESSAGE_CLIENT_INFO_APP_CONFIG_TENANT_ID_PROPERTY = "tenantId";
	public static final String MESSAGE_CLIENT_INFO_APP_CONFIG_APP_ID_PROPERTY = "appId";
	public static final String MESSAGE_CLIENT_INFO_APP_CONFIG_DEVICE_UUID_PROPERTY = "deviceUUID";
	public static final String MESSAGE_CLIENT_INFO_COUNTRY_CODE = "countryCode";
	public static final String MESSAGE_CLIENT_INFO_ORIGIN_COUNTRY = "originCountry";
		
    public static final String MESSAGE_CLIENT_INFO_IP_PROPERTY = "ip";
    
	@SerializedName(value = JsonMessage.MESSAGE_CLIENT_ID_PROPERTY)
	private String clientId;
	
	//profile
    @SerializedName(value = MESSAGE_CLIENT_INFO_PROFILE_USER_ID_PROPERTY)
	private String userId;
    @SerializedName(value = MESSAGE_CLIENT_INFO_PROFILE_LANGUAGE_PROPERTY)
	private String language;
    @SerializedName(value = MESSAGE_CLIENT_INFO_PROFILE_HANDICAP_PROPERTY)
	private Double handicap;
    @SerializedName(value = MESSAGE_CLIENT_INFO_PROFILE_ROLES_PROPERTY)
	private String[] roles;
    @SerializedName(value = MESSAGE_CLIENT_INFO_PROFILE_EMAILS_PROPERTY)
	private String[] emails;
    @SerializedName(value = MESSAGE_CLIENT_INFO_PROFILE_PHONES_PROPERTY)
	private String[] phones;
	
	//App config
    @SerializedName(value = MESSAGE_CLIENT_INFO_APP_CONFIG_TENANT_ID_PROPERTY)
	private String tenantId;
    @SerializedName(value = MESSAGE_CLIENT_INFO_APP_CONFIG_APP_ID_PROPERTY)
	private String appId;
    @SerializedName(value = MESSAGE_CLIENT_INFO_APP_CONFIG_DEVICE_UUID_PROPERTY)
	private String deviceUUID;
    @SerializedName(value = MESSAGE_CLIENT_INFO_COUNTRY_CODE)
	private ISOCountry countryCode;
    @SerializedName(value = MESSAGE_CLIENT_INFO_ORIGIN_COUNTRY)
	private ISOCountry originCountry;
	
	private String ip;
	
	public static ClientInfo byClientId(String clientId){
		final ClientInfo clientInfo = new ClientInfo();
		clientInfo.setClientId(clientId);
		return clientInfo;
	}

	public ClientInfo() {
	}

	public ClientInfo(String userId) {
		this.userId = userId;
	}
	
	public ClientInfo(String tenantId, String userId) {
		this.tenantId = tenantId;
		this.userId = userId;
	}

	public ClientInfo(String userId, String[] roles) {
		this.userId = userId;
		this.roles = roles;
	}
	
	public ClientInfo(String tenantId, String appId, String userId, String ip, Double handicap) {
		this.tenantId = tenantId;
		this.userId = userId;
		this.appId = appId;
		this.ip = ip;
		this.handicap = handicap;
	}
	
	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String[] getRoles() {
		return roles;
	}
	
	public void setRoles(String... roles) {
		this.roles = roles;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public Double getHandicap() {
		return handicap;
	}

	public void setHandicap(Double handicap) {
		this.handicap = handicap;
	}

	public String[] getEmails() {
		return emails;
	}

	public void setEmails(String[] emails) {
		this.emails = emails;
	}

	public String[] getPhones() {
		return phones;
	}

	public void setPhones(String[] phones) {
		this.phones = phones;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getDeviceUUID() {
		return deviceUUID;
	}

	public void setDeviceUUID(String deviceUUID) {
		this.deviceUUID = deviceUUID;
	}

	public String getIp() {
		return ip;
	}
	
	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public String getClientId() {
		return clientId;
	}
	
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	
	public ISOCountry getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(ISOCountry countryCode) {
		this.countryCode = countryCode;
	}

	public String getCountryCodeAsString() {
		String countryCodeString = null;
		if (this.countryCode != null) {
			countryCodeString = this.countryCode.toString();
		}
		return countryCodeString;
	}	
	
	public ISOCountry getOriginCountry() {
		return originCountry;
	}

	public void setOriginCountry(ISOCountry originCountry) {
		this.originCountry = originCountry;
	}

	public String getOriginCountryAsString() {
		String originCountryString = null;
		if (this.originCountry != null) {
			originCountryString = this.originCountry.toString();
		}
		return originCountryString;
	}	
	
	public boolean hasRoles() {
		return !ArrayUtils.isEmpty(roles);
	}

	public boolean hasRole(String tenantId, String roleName) {
		final String tenantPrefix = getTenantPrefix(tenantId);
		return Stream.of(ArrayUtils.nullToEmpty(roles)).anyMatch(r ->
			r.startsWith(TENANT_ROLE_PREFIX) && StringUtils.removeStart(r, tenantPrefix).equals(roleName) || 
			!r.startsWith(TENANT_ROLE_PREFIX) && r.equals(roleName)
		);
	}
	
	public boolean hasAnyRole(String tenantId, UserRole... roles){
		boolean hasRole = false;
		for (UserRole role : roles) {
			if(hasRole(tenantId, role.name())) {
				hasRole = true;
				break;
			}
		}
		return hasRole;
	}
	
	/**
	 * Checks if client info consists of more than client id
	 * @return true if more than client id is present
	 */
	public boolean hasUserInfo() {
		return !byClientId(clientId).equals(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((appId == null) ? 0 : appId.hashCode());
		result = prime * result + ((clientId == null) ? 0 : clientId.hashCode());
		result = prime * result + ((deviceUUID == null) ? 0 : deviceUUID.hashCode());
		result = prime * result + Arrays.hashCode(emails);
		result = prime * result + ((handicap == null) ? 0 : handicap.hashCode());
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result + ((language == null) ? 0 : language.hashCode());
		result = prime * result + Arrays.hashCode(phones);
		result = prime * result + Arrays.hashCode(roles);
		result = prime * result + ((tenantId == null) ? 0 : tenantId.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ClientInfo))
			return false;
		ClientInfo other = (ClientInfo) obj;
		if (appId == null) {
			if (other.appId != null)
				return false;
		} else if (!appId.equals(other.appId))
			return false;
		if (clientId == null) {
			if (other.clientId != null)
				return false;
		} else if (!clientId.equals(other.clientId))
			return false;
		if (deviceUUID == null) {
			if (other.deviceUUID != null)
				return false;
		} else if (!deviceUUID.equals(other.deviceUUID))
			return false;
		if (!Arrays.equals(emails, other.emails))
			return false;
		if (handicap == null) {
			if (other.handicap != null)
				return false;
		} else if (!handicap.equals(other.handicap))
			return false;
		if (ip == null) {
			if (other.ip != null)
				return false;
		} else if (!ip.equals(other.ip))
			return false;
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		if (!Arrays.equals(phones, other.phones))
			return false;
		if (!Arrays.equals(roles, other.roles))
			return false;
		if (tenantId == null) {
			if (other.tenantId != null)
				return false;
		} else if (!tenantId.equals(other.tenantId))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ClientInfo [clientId=").append(clientId);
		builder.append(", userId=").append(userId);
		builder.append(", language=").append(language);
		builder.append(", handicap=").append(handicap);
		builder.append(", roles=").append(Arrays.toString(roles));
		builder.append(", emails=").append(Arrays.toString(emails));
		builder.append(", phones=").append(Arrays.toString(phones));
		builder.append(", tenantId=").append(tenantId);
		builder.append(", appId=").append(appId);
		builder.append(", deviceUUID=").append(deviceUUID);
		builder.append(", countryCode=").append(countryCode);
		builder.append(", originCountry=").append(originCountry);
		builder.append(", ip=").append(ip).append("]");
		return builder.toString();
	}
	
	public static ClientInfo cloneOf(ClientInfo clientInfo) throws F4MFatalErrorException {
		try {
			if (clientInfo != null) {
				return (ClientInfo) BeanUtils.cloneBean(clientInfo);
			} else {
				return null;
			}
		} catch (IllegalAccessException | InstantiationException | InvocationTargetException
				| NoSuchMethodException e) {
			throw new F4MFatalErrorException("Failed to clone ClientInfo object", e);
		}
	}
	
	public static String getTenantPrefix(String tenantId){
		return TENANT_ROLE_PREFIX + tenantId + TENANT_ROLE_SEPARATOR;
	}
	
}
