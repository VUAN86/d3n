package de.ascendro.f4m.service.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceUriUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceUriUtil.class);

	public URI[] parseServiceUris(List<String> urisAsString) {
		final List<URI> uriList = getUriListFromUrisString(urisAsString);
		return !uriList.isEmpty() ? uriList.toArray(new URI[uriList.size()]) : null;
	}

	protected List<URI> getUriListFromUrisString(List<String> urisAsString) {
		final List<URI> uriList = new ArrayList<>();
		if (urisAsString != null) {
			for (String uriString : urisAsString) {
				try {
					uriList.add(URI.create(uriString));
				} catch (NullPointerException | IllegalArgumentException e) {
					LOGGER.error("Cannot build service URI from {}: {}", uriString, e);
				}
			}
		}
		return uriList;
	}
}
