package de.ascendro.f4m.service.json.model.type;

import java.util.Set;

import org.everit.json.schema.Schema;

public interface JsonMessageSchemaMap {

	Schema register(String namespace, Schema schema);

	Schema get(String namespace);

	/**
	 * Retrieve message related roles
	 * @param messageName
	 * @return set of roles
	 */
	Set<String> getMessageRoles(String messageName);

	/**
	 * Retrieve message related permissions
	 * @param messageName - JSON message name 
	 * @return set of permissions
	 */
	Set<String> getMessagePermissions(String messageName);
}
