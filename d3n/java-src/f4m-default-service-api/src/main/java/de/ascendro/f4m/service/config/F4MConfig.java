package de.ascendro.f4m.service.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;

public abstract class F4MConfig implements Config {
	private static final Logger LOGGER = LoggerFactory.getLogger(F4MConfig.class);

	private static final String LIST_ITEM_SEPARATOR = ",";

	private static final String F4M_CONF_PROPERTY = "f4m.conf";
	public static final String F4M_PROTOCOL_LOGGING = "f4m.protocolLogging";
	public static final String F4M_HEARTBEAT_LOGGING = "f4m.heartbeatLogging";

	public static final String SERVICE_HOST = "service.host";
	public static final String SERVICE_NAME = "service.name";
	public static final String SERVICE_NAMESPACES = "service.namespaces";

	public static final String SERVICE_DEPENDENT_SERVICES = "service.dependentServices";
	public static final String PROMOCODE_SYSTEM_REST_URL = "https://quizdom.cbvalidate.de";
	
	protected final Properties properties = createProperties();

	@Override
	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}

	@Override
	public void setProperty(String key, boolean value) {
		properties.setProperty(key, Boolean.toString(value));
	}

	@Override
	public void setProperty(String key, Number value) {
		properties.setProperty(key, String.valueOf(value));
	}
	
	@Override
	public void setProperty(String key, URL resource) {
		try {
			setProperty(key, Resources.toString(resource, Charsets.UTF_8));
		} catch (IOException e) {
			throw new F4MFatalErrorException(String.format("Failed to read resource [%s]", resource), e);
		}
	}

	@Override
	public String getProperty(String key) {
		return properties.getProperty(key);
	}

	@Override
	public Integer getPropertyAsInteger(String key) {
		final String propertyAsString = properties.getProperty(key);

		final Integer propertyAsInteger;
		if (!StringUtils.isBlank(propertyAsString)) {
			propertyAsInteger = Integer.valueOf(propertyAsString);
		} else {
			propertyAsInteger = null;
		}
		return propertyAsInteger;
	}

	@Override
	public Double getPropertyAsDouble(String key) {
		final String propertyAsString = properties.getProperty(key);

		final Double propertyAsDouble;
		if (!StringUtils.isBlank(propertyAsString)) {
			propertyAsDouble = Double.valueOf(propertyAsString);
		} else {
			propertyAsDouble = null;
		}
		return propertyAsDouble;
	}

	@Override
	public BigDecimal getPropertyAsBigDecimal(String key) {
		final String propertyAsString = properties.getProperty(key);
		return StringUtils.isBlank(propertyAsString) ? null 
				: new BigDecimal(propertyAsString);
	}

	@Override
	public Boolean getPropertyAsBoolean(String key) {
		final String value = properties.getProperty(key);
		return value != null ? Boolean.valueOf(value) : null;
	}

	@Override
	public Long getPropertyAsLong(String key) {
		final String propertyAsString = properties.getProperty(key);

		final Long propertyAsLong;
		if (!StringUtils.isBlank(propertyAsString)) {
			propertyAsLong = Long.valueOf(propertyAsString);
		} else {
			propertyAsLong = null;
		}
		return propertyAsLong;
	}

	@Override
	public void setProperty(String key, List<String> values) {
		final String valuesAsString = values.stream().collect(Collectors.joining(LIST_ITEM_SEPARATOR));
		setProperty(key, valuesAsString);
	}

	@Override
	public List<String> getPropertyAsListOfStrings(String key) {
		final String valuesAsString = getProperty(key);

		final List<String> values;
		if (valuesAsString != null) {
			if (!StringUtils.isEmpty(valuesAsString)) {
				values = Arrays.asList(valuesAsString.split(LIST_ITEM_SEPARATOR));
			} else {
				values = Collections.emptyList();
			}
		} else {
			values = null;
		}
		return values;
	}

	protected Properties createProperties() {
		final Properties prop = new Properties();
		prop.putAll(System.getProperties());
		return prop;
	}

	protected void loadProperties() {
		final Properties newProperties = createProperties();
		final String propertyFilePath = System.getProperty(F4M_CONF_PROPERTY);
		if (StringUtils.isNotEmpty(propertyFilePath)) {
			final File f4mConfFile = new File(propertyFilePath);

			try (final InputStream f4mConfFileInputStream = new FileInputStream(new File(propertyFilePath))) {
				properties.load(f4mConfFileInputStream);
			} catch (FileNotFoundException fnEx) {
				LOGGER.error("F4M.conf file not found at[" + f4mConfFile.getAbsolutePath() + "]: " + fnEx);
			} catch (IOException ioEx) {
				LOGGER.error("Cannot load F4M.conf file at[" + f4mConfFile.getAbsolutePath() + "]: " + ioEx);
			}
		} else {
			LOGGER.debug("Skip F4M_CONF_PROPERTY[" + F4M_CONF_PROPERTY + "] - not set");
		}
		this.properties.putAll(newProperties);
	}

	@Override
	public Properties getProperties() {
		return properties;
	}

}
