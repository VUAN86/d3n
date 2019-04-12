package de.ascendro.f4m.server.country.nogambling;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import de.ascendro.f4m.server.config.AerospikeConfigImpl;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.json.model.user.ClientInfo;

public class NoGamblingCountry {
	private Config config;
	
	@Inject
	public NoGamblingCountry(Config config) {
		this.config = config;
	}

	/**
	 *checks if user does not come from country where gambling is prohibited.
	 */
	public boolean userComesFromNonGamblingCountry(ClientInfo clientInfo) {

		final String countryListString = config.getProperty(AerospikeConfigImpl.NO_GAMBLING_COUNTRY_LIST);
		boolean canUserGamble = true;
		List<String> noGamblingCountryList = Arrays.asList(countryListString.split(","));

		if (clientInfo.getCountryCode() == null || noGamblingCountryList.contains(clientInfo.getCountryCode().name())) {
			canUserGamble = false;
		}
		return canUserGamble;
	}
	
}
