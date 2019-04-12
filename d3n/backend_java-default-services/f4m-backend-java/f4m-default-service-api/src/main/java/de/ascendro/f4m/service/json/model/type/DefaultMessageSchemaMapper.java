package de.ascendro.f4m.service.json.model.type;

/**
 * Default JSON Schema Map with all needed schema for most of services.<br>
 * Authentication schema map is included by default.<br>
 * Authentication schema is located in AUTH_SCHEMA_PATH;
 */
public class DefaultMessageSchemaMapper extends JsonMessageSchemaMapImpl {
	private static final long serialVersionUID = -5328805275763892364L;

	private static final String AUTH_SCHEMA_PATH = "auth.json";
	private static final String REGISTRY_SCHEMA_PATH = "sreg.json";
	private static final String EVENT_SCHEMA_PATH = "event.json";
	private static final String GATEWAY_SCHEMA_PATH = "gateway.json";
	
	/**
	 * Constructor load and initialize all default schemas.
	 */
	public DefaultMessageSchemaMapper() {
		init();
	}

	protected void init() {
		this.register(DefaultMessageSchemaMapper.class, "Auth", AUTH_SCHEMA_PATH);
		this.register(DefaultMessageSchemaMapper.class, "serviceRegistry", REGISTRY_SCHEMA_PATH);
		this.register(DefaultMessageSchemaMapper.class, "event", EVENT_SCHEMA_PATH);
		this.register(DefaultMessageSchemaMapper.class, "gateway", GATEWAY_SCHEMA_PATH);
	}

}
