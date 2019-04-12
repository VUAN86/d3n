package de.ascendro.f4m.service.promocode.rest.wrapper;

import java.net.*;
import java.io.*;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.promocode.config.PromocodeConfig;

public abstract class RestWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(RestWrapper.class);

	private URI getURI(String partUri) {
		String baseURL = getRestUrl();
		if (StringUtils.isBlank(baseURL)) {
			throw new F4MFatalErrorException("Promocode URL not set");
		}
		UriBuilder builder = UriBuilder.fromUri(baseURL).path(partUri);
		return builder.build();
	}


	private HttpURLConnection getTarget(String partUri) {
		HttpURLConnection connection = null;
		try {
			//Create connection
			String uri = getURI(partUri).toString();
			URL url = new URL(uri);

			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setUseCaches(false);
			connection.setDoOutput(true);
			return connection;
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}


	protected String callGet(String partUri) {

		HttpURLConnection connection = null;
		try {
			connection = getTarget(partUri);
			Stopwatch stopwatch = Stopwatch.createStarted();

			//Get Response
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
			String line;
			while ((line = rd.readLine()) != null) {
				response.append(line);
			}
			rd.close();
			LOGGER.debug("GET {} executed in {} with response {}", connection, stopwatch.stop(), response);
			return response.toString();
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	protected String getRestUrl() {
		return PromocodeConfig.PROMOCODE_SYSTEM_REST_URL;
	}
}
