package de.ascendro.f4m.service.winning.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;

public class InsuranceInterfaceWrapper {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(InsuranceInterfaceWrapper.class);

	public static final String RESPONSE_LOST = "0";
	public static final String RESPONSE_WON = "1";
	public static final String RESPONSE_INVALID_PARAMETERS = "-1";
	public static final String RESPONSE_INTERNAL_ERROR = "-2";

	public boolean callInsuranceApi(String apiUrl) {
		try {
			URL url = new URL(apiUrl);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			int responseCode = con.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				LOGGER.error("Unsuccessful GET call to {}, responseCode {}", apiUrl, responseCode);
				throw new F4MFatalErrorException("Invalid insurance API response code");
			}
			try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				String response = in.readLine();
				if (RESPONSE_LOST.equals(response)) {
					return false;
				} else if (RESPONSE_WON.equals(response)) {
					return true;
				} else {
					LOGGER.error("Invalid response {} to GET call to {}", response, apiUrl);
					throw new F4MFatalErrorException("Invalid insurance API response");
				}
			}
		} catch (IOException e) {
			LOGGER.error("Unsuccessful GET call to {}", apiUrl);
			throw new F4MFatalErrorException("Could not read insurance API", e);
		}
	}

	

}
