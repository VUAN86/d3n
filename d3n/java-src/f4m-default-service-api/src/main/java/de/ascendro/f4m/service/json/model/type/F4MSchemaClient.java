package de.ascendro.f4m.service.json.model.type;

import java.io.InputStream;

import org.everit.json.schema.loader.internal.DefaultSchemaClient;

/**
 * Schema client for F4M schemas, loading default schemas from common path.
 */
public class F4MSchemaClient extends DefaultSchemaClient {

	private static final String DEFINITIONS_PREFIX = "definitions/";
	private static final String MODELS_PREFIX = "models/";
	
	@Override
	public InputStream get(final String url) {
		if (url.startsWith(DEFINITIONS_PREFIX) || url.startsWith(MODELS_PREFIX)) {
			return F4MSchemaClient.class.getClassLoader().getResourceAsStream(url);
		} else {
			return super.get(url);
		}
	}

}
