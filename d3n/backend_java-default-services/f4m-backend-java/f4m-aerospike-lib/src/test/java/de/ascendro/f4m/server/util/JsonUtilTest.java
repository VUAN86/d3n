package de.ascendro.f4m.server.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.OrderBy;
import de.ascendro.f4m.service.json.model.OrderBy.Direction;

public class JsonUtilTest {

	private static final Gson gson = new Gson();

	@Test
	public void testSort() {
		List<JsonElement> list = Arrays.asList(
				gson.fromJson("{\"userId\": \"3\",\"person\": {\"firstName\": \"Name2\",\"lastName\": \"Surname1\"}}", JsonElement.class),
				gson.fromJson("{\"userId\": \"2\",\"person\": {\"firstName\": \"Name1\",\"lastName\": \"Surname2\"}}", JsonElement.class),
				gson.fromJson("{\"userId\": \"1\",\"person\": {\"firstName\": \"Name3\",\"lastName\": \"Surname3\"}}", JsonElement.class),
				gson.fromJson("{\"userId\": \"4\",\"person\": {\"firstName\": \"Name3\",\"lastName\": \"Surname0\"}}", JsonElement.class),
				gson.fromJson("{\"userId\": \"5\",\"person\": {\"firstName\": \"Name3\"}}", JsonElement.class));

		// Sort by userId
		JsonUtil.sort(list, Arrays.asList(new OrderBy("userId", Direction.asc)));
		assertListContains(list, 1, 2, 3, 4, 5);
		JsonUtil.sort(list, Arrays.asList(new OrderBy("userId", Direction.desc)));
		assertListContains(list, 5, 4, 3, 2, 1);

		// Sort by person.firstName and person.lastName
		JsonUtil.sort(list, Arrays.asList(new OrderBy("person.firstName", Direction.asc),
				new OrderBy("person.lastName", Direction.asc)));
		assertListContains(list, 2, 3, 5, 4, 1);
		JsonUtil.sort(list, Arrays.asList(new OrderBy("person.firstName", Direction.asc),
				new OrderBy("person.lastName", Direction.desc)));
		assertListContains(list, 2, 3, 1, 4, 5);
		JsonUtil.sort(list, Arrays.asList(new OrderBy("person.firstName", Direction.desc),
				new OrderBy("person.lastName", Direction.asc)));
		assertListContains(list, 5, 4, 1, 3, 2);
		JsonUtil.sort(list, Arrays.asList(new OrderBy("person.firstName", Direction.desc),
				new OrderBy("person.lastName", Direction.desc)));
		assertListContains(list, 1, 4, 5, 3, 2);

		// empty sort (sort by object value ascending)
		JsonUtil.sort(list, Collections.emptyList());
		assertListContains(list, 1, 2, 3, 4, 5);

		// non-existing property (left unsorted, since results considered equal)
		JsonUtil.sort(list, Arrays.asList(new OrderBy("xxx", Direction.asc), new OrderBy("xxx", Direction.desc)));
		assertListContains(list, 1, 2, 3, 4, 5);
		JsonUtil.sort(list, Arrays.asList(new OrderBy("xxx", Direction.desc), new OrderBy("xxx", Direction.asc)));
		assertListContains(list, 1, 2, 3, 4, 5);

		list = Arrays.asList(
				gson.fromJson("[\"3\",\"person\"]", JsonElement.class), 
				gson.fromJson("[\"2\",\"person\"]", JsonElement.class),
				gson.fromJson("[\"1\",\"person\"]", JsonElement.class), 
				gson.fromJson("[\"4\",\"person\"]", JsonElement.class));

		// non-object elements sorted by direction specified in first orderBy
		JsonUtil.sort(list, Arrays.asList(new OrderBy("xxx", Direction.asc), new OrderBy("xxx", Direction.desc)));
		assertPrimitiveOrder(list, 1, 2, 3, 4);
		JsonUtil.sort(list, Arrays.asList(new OrderBy("xxx", Direction.desc), new OrderBy("xxx", Direction.asc)));
		assertPrimitiveOrder(list, 4, 3, 2, 1);
	}

	private void assertPrimitiveOrder(List<JsonElement> list, int... ids) {
		for (int i = 0; i < ids.length; i++) {
			assertEquals("Element <" + i + ">", "[\"" + ids[i] + "\",\"person\"]", list.get(i).toString());
		}
	}

	private void assertListContains(List<? extends JsonElement> list, int... ids) {
		assertEquals("Wrong number of results; ", ids.length, list.size());
		for (int i = 0; i < ids.length; i++) {
			assertEquals("Element <" + i + "> expected with userId=" + ids[i], String.valueOf(ids[i]),
					((JsonObject) list.get(i)).get("userId").getAsString());
		}
	}

	@Test
	public void testFilter() {
		List<JsonObject> list = Arrays.asList(
				gson.fromJson("{\"userId\": \"3\",\"roles\":[\"role1\",\"role2\"], \"person\": {\"firstName\": \"Abrakadabra\",\"lastName\": \"Surname1\"}}", JsonObject.class),
				gson.fromJson("{\"userId\": \"2\",\"roles\":[\"role2\"],\"person\": {\"firstName\": \"Mosquito\",\"lastName\": \"Surname2\"}}", JsonObject.class),
				gson.fromJson("{\"userId\": \"1\",\"person\": {\"firstName\": \"Facelifter\",\"lastName\": \"Surname3\"}}", JsonObject.class),
				gson.fromJson("{\"userId\": \"4\",\"roles\":[\"role2\",\"role3\"],\"person\": {\"firstName\": \"MooShoppingmaller\",\"lastName\": \"Surname0\"}}", JsonObject.class),
				gson.fromJson("{\"userId\": \"5\",\"person\": {\"firstName\": \"Forte\"}}", JsonObject.class));

		// Test filtering simple property
		Map<String, String> filter = new HashMap<String, String>();
		filter.put("person.firstName", "abrakadabra");
		assertListContains(JsonUtil.filter(list, filter), 3);
		filter.put("person.firstName", "*i*r");
		assertListContains(JsonUtil.filter(list, filter), 1, 4);
		filter.put("person.firstName", "m?s*");
		assertListContains(JsonUtil.filter(list, filter), 2);
		filter.put("person.firstName", "m*s*");
		assertListContains(JsonUtil.filter(list, filter), 2, 4);
		filter.put("person.firstName", "m*oos*");
		assertListContains(JsonUtil.filter(list, filter), 4);
		
		// Test filter for array properties
		filter = new HashMap<String, String>();
		filter.put("roles", "role1");
		assertListContains(JsonUtil.filter(list, filter), 3);
		filter.put("roles", "role2");
		assertListContains(JsonUtil.filter(list, filter), 3, 2, 4);
		filter.put("roles", "ROLE3");
		assertListContains(JsonUtil.filter(list, filter), 4);
	}

	@Test
	public void testExtractParts() {
		JsonObject object = gson.fromJson(
				"{\"userId\": \"3\",\"person\": {\"firstName\": \"Abrakadabra\",\"lastName\": \"Surname1\"},\"xxx\": \"yyy\"}",
				JsonObject.class);
		assertEquals("{}", JsonUtil.extractParts(object, "foo").toString());
		assertEquals("{\"userId\":\"3\"}", JsonUtil.extractParts(object, "userId").toString());
		assertEquals("{\"userId\":\"3\",\"person\":{\"firstName\":\"Abrakadabra\",\"lastName\":\"Surname1\"}}",
				JsonUtil.extractParts(object, "userId", "person").toString());
		assertEquals("{\"userId\":\"3\",\"person\":{\"firstName\":\"Abrakadabra\",\"lastName\":\"Surname1\"},\"xxx\":\"yyy\"}",
				JsonUtil.extractParts(object).toString());
	}
	
}
