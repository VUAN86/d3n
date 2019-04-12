package de.ascendro.f4m.service.json.model.type;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.io.IOUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.exception.server.F4MIOException;

/**
 * JSON Message Schema superclass, with implemented methods to load and register schemas. 
 */
public class JsonMessageSchemaMapImpl extends HashMap<String, Schema> implements JsonMessageSchemaMap {

	private static final long serialVersionUID = -3262029691259162660L;

	private static final Logger LOGGER = LoggerFactory.getLogger(JsonMessageSchemaMapImpl.class);

	private static final String X_PERMISSIONS = "x-permissions";
	private static final String DEFINITIONS_NAME = "definitions";
	private static final String PATHS_NAME = "paths";
	private static final String SECURITY_ROLES_JSON = "security/roles.json";

	/**
	 * Mapping between user roles and permissions:
	 * { <<role>> : [<<permissions>>] }
	 */
	private final Map<String, Set<String>> rolePermissions;
	
	/**
	 * Mapping between message and required permissions
	 * { <<schema/name>> : [<<permissions>>] }
	 */
	private final Map<String, Set<String>> messagePermissions = new HashMap<>();
	
	/*
	 * Cross referenced mapping between messages and required roles
	 * { <<schema/name>> : [<<roles>>] }
	 */
	private final Map<String, Set<String>> messageRoles = new HashMap<>();
	
	public JsonMessageSchemaMapImpl() {
		this.rolePermissions = Collections.unmodifiableMap(loadRolePermissions());
	}
	
	/**
	 * Registers schema to schemas name.
	 * @param schemaName
	 * @param schema
	 */
	@Override
	public Schema register(String schemaName, Schema schema) {
		return put(schemaName, schema);
	}

	/**
	 * Registers all schemas using schema file provided by schemaPath.
	 * @param aClass
	 * @param classpath
	 * @param schemaPath
	 * @return Schema
	 */	
	public Schema register(Class<?> aClass, String classpath, String schemaPath) {
		return loadSchema(aClass, classpath, schemaPath);
	}

	@Override
	public Schema put(String key, Schema value) {
		return super.put(key.toLowerCase(), value);
	}
	
	@Override
	public Schema get(String namespace) {
		Schema schema;
		if (namespace != null) {
			schema = super.get(namespace.toLowerCase());
		} else {
			schema = null;
		}
		return schema;
	}

	protected Schema loadSchema(Class<?> aClass, String classpath, String path) {
		try (InputStream resource = aClass.getClassLoader().getResourceAsStream(path)) {
			JSONObject swaggerDef = new JSONObject(new JSONTokener(resource));
			loadMessageDefinitions(classpath, (JSONObject) swaggerDef.get(DEFINITIONS_NAME));
			loadMessagePermissions((JSONObject) swaggerDef.get(PATHS_NAME));
			crossReferenceMessagesWithRoles();
		} catch (IOException e) {
			LOGGER.error("Could not load JSON shema from {}", path, e);
		}
		return null;
	}

	protected void crossReferenceMessagesWithRoles() {
		for(Map.Entry<String, Set<String>> messageAndPermissions : messagePermissions.entrySet()){
			messageRoles.put(messageAndPermissions.getKey(), new HashSet<>());
			for (String permission : messageAndPermissions.getValue()) {
				final String[] messagePermissionRoles = rolePermissions.entrySet().stream()
					.filter(entry -> entry.getValue().contains(permission))
					.map(Map.Entry::getKey)
					.toArray(String[]::new);
				Collections.addAll(messageRoles.get(messageAndPermissions.getKey()), messagePermissionRoles);
			}
		}
	}

	private void loadMessagePermissions(JSONObject paths) {
		for (String path : paths.keySet()) {
			final JSONObject methodPathSchema = Optional.ofNullable(paths.optJSONObject(path))
					.orElse(new JSONObject());
			for(String method : methodPathSchema.keySet()){
				final JSONObject messagePathSchema = Optional.ofNullable(methodPathSchema.optJSONObject(method))
						.orElse(new JSONObject());
				
				final JSONArray permissions = Optional.ofNullable(messagePathSchema.optJSONArray(X_PERMISSIONS))
						.orElse(new JSONArray());
				if(permissions.length() > 0){
					messagePermissions.put(path.substring(1),  StreamSupport.stream(permissions.spliterator(), false)
							.map(p -> (String) p)
							.collect(Collectors.toSet()));
				}				
			}
		}
	}

	private void loadMessageDefinitions(String classpath, JSONObject definitions) {
		for (String definitionName : definitions.keySet()) {
			JSONObject messageSchema = (JSONObject) definitions.get(definitionName);
			
			// Make a copy in order to modify without affecting original
			JSONObject messageSchemaCopy = new JSONObject(messageSchema, JSONObject.getNames(messageSchema));
			
			// Add all other definitions in order to support references to other definitions
			messageSchemaCopy.put(DEFINITIONS_NAME, definitions);
			
			// Build schema
			Schema schema = SchemaLoader.load(messageSchemaCopy, new F4MSchemaClient());
			put(getSchemaName(classpath, definitionName), schema);
		}
	}

	private String getSchemaName(String classpath, String name) {
		String schemaName;
		if (name != null && name.startsWith(classpath + "/")) {
			schemaName = name;
		} else {
			schemaName = classpath + "/" + name;
		}
		return schemaName;
	}
		
	/**
	 * Load permissions for roles from JSON Schemas
	 * @return Map <role, Set<permission>>, empty map in case of missing resources
	 */
	protected Map<String, Set<String>> loadRolePermissions(){
		Map<String, Set<String>> permissionMapping = Collections.emptyMap();
		String rolesJsonAsString = null;
		try (InputStream resource = this.getClass().getClassLoader().getResourceAsStream(SECURITY_ROLES_JSON)) {
			if (resource != null) {
				rolesJsonAsString = IOUtils.toString(resource);
				final Type type = new TypeToken<Map<String, Set<String>>>() {
				}.getType();
				permissionMapping = new Gson().fromJson(rolesJsonAsString, type);
			} else {
				throw new F4MIOException(String.format("Roles-permissions mapping resource [%s] not found", SECURITY_ROLES_JSON));
			}
		} catch (IOException e) {
			LOGGER.error("Could not load role permissions [{}] mapping from JSON shemas ", SECURITY_ROLES_JSON, e);
		} catch (JsonParseException jsonEx) {
			LOGGER.error("Failed to parse role permissions mapping at [{}] with contents [{}]", SECURITY_ROLES_JSON,
					rolesJsonAsString, jsonEx);
		}
		return permissionMapping;
	}
	
	public Map<String, Set<String>> getRolePermissions() {
		return rolePermissions;
	}
	
	public Set<String> getRolePermissions(String[] roles) {
		return Stream.of(roles)
			.map(r -> Optional.ofNullable(rolePermissions.get(r))
					.orElse(Collections.emptySet()))
			.flatMap(Set::stream)
			.collect(Collectors.toSet());
	}
	
	public Map<String, Set<String>> getMessagePermissions() {
		return messagePermissions;
	}
	
	public Map<String, Set<String>> getMessageRoles() {
		return messageRoles;
	}
	
	@Override
	public Set<String> getMessageRoles(String messageName) {
		return getMessageRoles().get(messageName);
	}
	
	@Override
	public Set<String> getMessagePermissions(String messageName) {
		return Optional.ofNullable(getMessagePermissions().get(messageName))
				.orElse(Collections.emptySet());
	}
	
}