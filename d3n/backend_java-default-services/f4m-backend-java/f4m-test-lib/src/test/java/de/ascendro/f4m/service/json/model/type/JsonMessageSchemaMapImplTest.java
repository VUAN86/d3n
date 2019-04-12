package de.ascendro.f4m.service.json.model.type;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Test;

import de.ascendro.f4m.service.json.model.user.UserPermission;
import de.ascendro.f4m.service.json.model.user.UserRole;
import de.ascendro.f4m.service.ping.PingPongMessageSchemaMapper;
import de.ascendro.f4m.service.ping.model.JsonPingMessageContent;

public class JsonMessageSchemaMapImplTest {
	
	@Test
	public void testLoadRolePermissions() {
		final Map<String, Set<String>> rolePermissions = new PingPongMessageSchemaMapper().getRolePermissions();

		Stream.of(UserRole.values()).forEach(r -> {
			assertTrue("Roles' permissions mapping does not contain role " + r, rolePermissions.containsKey(r.name()));
			assertNotNull("Roles' permissions mapping is null for role " + r, rolePermissions.get(r.name()));
			assertThat(rolePermissions.get(r.name()), not(emptyIterable()));
		});
	}

	@Test
	public void testLoadMessagePermissions() {
		final Map<String, Set<String>> messagePermissions = new PingPongMessageSchemaMapper().getMessagePermissions();
		assertThat(messagePermissions.get(JsonPingMessageContent.MESSAGE_WITH_PERMISSIONS_NAME),
				containsInAnyOrder(UserPermission.TENANT_DATA_WRITE.name(), UserPermission.INTERNAL_DATA_WRITE.name()));
		assertNull(messagePermissions.get(JsonPingMessageContent.MESSAGE_NAME));
	}

	@Test
	public void testCrossReferenceMessagesWithRoles() {
		final List<String> registeredPermissions = Arrays.asList(UserPermission.TENANT_DATA_WRITE.name(),
				UserPermission.INTERNAL_DATA_WRITE.name());
		final Map<String, Set<String>> rolePermissions = new HashMap<>();
		rolePermissions.put(UserRole.REGISTERED.name(), new HashSet<>(registeredPermissions));
		
		final Map<String, Set<String>> messageRoles = createJsonMessageSchemaMap(rolePermissions).getMessageRoles();
			
		assertThat(messageRoles.get(JsonPingMessageContent.MESSAGE_WITH_PERMISSIONS_NAME),
				containsInAnyOrder(UserRole.REGISTERED.name()));
		assertNull(messageRoles.get(JsonPingMessageContent.MESSAGE_NAME));
	}
	
	@Test
	public void testCrossReferenceMessagesWithRolesForMissingRoles() {
		final Map<String, Set<String>> messageRoles = createJsonMessageSchemaMap(new HashMap<>())
				.getMessageRoles();
			
		assertThat(messageRoles.get(JsonPingMessageContent.MESSAGE_WITH_PERMISSIONS_NAME), 
				emptyIterable());
		assertNull(messageRoles.get(JsonPingMessageContent.MESSAGE_NAME));
	}
	
	private JsonMessageSchemaMapImpl createJsonMessageSchemaMap( Map<String, Set<String>> rolePermissions) {
		return new PingPongMessageSchemaMapper(){
			private static final long serialVersionUID = -3755178321064930074L;

			@Override
			protected Map<String, Set<String>> loadRolePermissions() {
				return rolePermissions;
			}
		};
	}

}
