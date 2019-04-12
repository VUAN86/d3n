package de.ascendro.f4m.service.config;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.Properties;

public interface Config {
	public static final String TRUST_STORE = "javax.net.ssl.trustStore";
	public static final String TRUST_STORE_PASSWORD = "javax.net.ssl.trustStorePassword";
	public static final String KEY_STORE = "javax.net.ssl.keyStore";
	public static final String KEY_STORE_PASSWORD = "javax.net.ssl.keyStorePassword";

	void setProperty(String key, String value);
	
	void setProperty(String key, boolean value);
	
	void setProperty(String key, List<String> values);

	void setProperty(String key, Number value);
	
	void setProperty(String key, URL resource);

	String getProperty(String key);

	Integer getPropertyAsInteger(String key);

	Double getPropertyAsDouble(String key);

	BigDecimal getPropertyAsBigDecimal(String key);
	
	Boolean getPropertyAsBoolean(String key);
	
	Long getPropertyAsLong(String key);
	
	List<String> getPropertyAsListOfStrings(String key);

	Properties getProperties();
}
