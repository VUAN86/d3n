package de.ascendro.f4m.service.json.model;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public class JsonObjectWrapperTest {

	private static final String ARRAY_PROPERTY = "array";

	private JsonObjectWrapper jsonObject;

	@Before
	public void setUp() throws Exception {
		jsonObject = new JsonObjectWrapper();
	}

	@Test
	public void testSetElementOfArray() {
		assertThat(jsonObject.getArray(ARRAY_PROPERTY), nullValue());

		// invalid index
		jsonObject.setElementOfArray(ARRAY_PROPERTY, -1, new JsonPrimitive(13),false);
		assertThat(jsonObject.getArray(ARRAY_PROPERTY), nullValue());

		// set element at index 2
		jsonObject.setElementOfArray(ARRAY_PROPERTY, 2, new JsonPrimitive(13),false);
		assertThat(jsonObject.getArray(ARRAY_PROPERTY).get(0), equalTo(JsonNull.INSTANCE));
		assertThat(jsonObject.getArray(ARRAY_PROPERTY).get(1), equalTo(JsonNull.INSTANCE));
		assertThat(jsonObject.getArray(ARRAY_PROPERTY).get(2).getAsInt(), equalTo(13));

		// set element at index 0
		jsonObject.setElementOfArray(ARRAY_PROPERTY, 0, new JsonPrimitive(42),false);
		assertThat(jsonObject.getArray(ARRAY_PROPERTY).get(0).getAsInt(), equalTo(42));
		assertThat(jsonObject.getArray(ARRAY_PROPERTY).get(1), equalTo(JsonNull.INSTANCE));
		assertThat(jsonObject.getArray(ARRAY_PROPERTY).get(2).getAsInt(), equalTo(13));
		
		// set element at index 4
		jsonObject.setElementOfArray(ARRAY_PROPERTY, 4, new JsonPrimitive(-8),false);
		assertThat(jsonObject.getArray(ARRAY_PROPERTY).get(0).getAsInt(), equalTo(42));
		assertThat(jsonObject.getArray(ARRAY_PROPERTY).get(1), equalTo(JsonNull.INSTANCE));
		assertThat(jsonObject.getArray(ARRAY_PROPERTY).get(2).getAsInt(), equalTo(13));
		assertThat(jsonObject.getArray(ARRAY_PROPERTY).get(3), equalTo(JsonNull.INSTANCE));
		assertThat(jsonObject.getArray(ARRAY_PROPERTY).get(4).getAsInt(), equalTo(-8));
	}

}
