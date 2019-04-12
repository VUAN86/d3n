package de.ascendro.f4m.service.json.model;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import de.ascendro.f4m.service.util.DateTimeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Json Object Wrapper with common functionality of other Json Objects.
 * This class is intended to be used only for cases, when whole list of attributes is not known and no attribute should be lost.
 * For cases, where exact contents of JSON is known and all attributes are used in Java, POJO should be preferred.
 * 
 * Avoid using JsonObjectWrapper for complex JSON objects with lot of attributes, 
 * since copy/paste errors might occur without code generation support, which is available for POJO.
 */
public class JsonObjectWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonObjectWrapper.class);

	protected JsonObject jsonObject;

	public JsonObjectWrapper() {
		this(new JsonObject());
	}

	public JsonObjectWrapper(JsonObject jsonObject) throws IllegalArgumentException {
		this.jsonObject = jsonObject;
		if (jsonObject == null) {
			throw new IllegalArgumentException("Json Object must not be null");
		}
	}

	public JsonObject getJsonObject() {
		return jsonObject;
	}

	public String getAsString() {
		return jsonObject != null ? jsonObject.toString() : null;
	}

	/**
	 * 
	 * @param propertyName
	 *            {@link String}
	 * @return property value as {@link String}
	 */
	public String getPropertyAsString(String propertyName) {
		String value = null;

		final JsonElement element = jsonObject.get(propertyName);
		if (element != null && !element.isJsonNull()) {
			value = element.getAsString();
		}

		return value;
	}
	
	public ZonedDateTime getPropertyAsZonedDateTime(String propertyName){
		final String dateTimeAsString = getPropertyAsString(propertyName);
		final ZonedDateTime dateTime;
		if (dateTimeAsString != null) {
			dateTime = DateTimeUtil.parseISODateTimeString(dateTimeAsString);
		} else{
			dateTime = null;
		}
		return dateTime;
	}

	public JsonObject getPropertyAsJsonObject(String propertyName) {
		JsonObject value = null;

		final JsonElement element = jsonObject.get(propertyName);
		if (element != null && !element.isJsonNull() && element.isJsonObject()) {
			value = element.getAsJsonObject();
		}

		return value;
	}

	public JsonArray getPropertyAsJsonArray(String propertyName) {
		JsonArray value = null;

		final JsonElement element = jsonObject.get(propertyName);
		if (element != null && !element.isJsonNull() && element.isJsonArray()) {
			value = element.getAsJsonArray();
		}

		return value;
	}

	/**
	 * 
	 * @param propertyName
	 *            {@link String}
	 * @return property value as {@link Boolean}
	 */
	public Boolean getPropertyAsBoolean(String propertyName) {
		Boolean value = null;

		final JsonElement element = jsonObject.get(propertyName);
		if (element != null && !element.isJsonNull()) {
			value = element.getAsBoolean();
		}

		return value;
	}

	/**
	 * Get property as int. 0 - if no property value found
	 * 
	 * @param propertyName
	 *            {@link String}
	 * @return property value as {@link int}
	 */
	public int getPropertyAsInt(String propertyName) {
		final Integer value = getPropertyAsInteger(propertyName);
		return value != null ? value : 0;
	}

	public long getPropertyAsLong(String propertyName) {
		final Long value = getPropertyAsLongObject(propertyName);
		return value != null ? value : 0L;
	}

	public Integer getPropertyAsInteger(String propertyName) {
		Integer value = null;

		final JsonElement element = jsonObject.get(propertyName);
		if (element != null && !element.isJsonNull()) {
			value = element.getAsInt();
		}

		return value;
	}

	public Long getPropertyAsLongObject(String propertyName) {
		Long value = null;

		final JsonElement element = jsonObject.get(propertyName);
		if (element != null && !element.isJsonNull()) {
			value = element.getAsLong();
		}

		return value;
	}

	/**
	 * 
	 * @param propertyName
	 *            {@link String}
	 * @return property value as {@link double}
	 */
	public double getPropertyAsDouble(String propertyName) {
		double value = 0.0;

		final JsonElement element = jsonObject.get(propertyName);
		if (element != null && !element.isJsonNull()) {
			value = element.getAsDouble();
		}

		return value;
	}

	/**
	 * 
	 * @param propertyName
	 *            {@link String}
	 * @return property value as {@link Double}
	 */
	public Double getPropertyAsDoubleObject(String propertyName) {
		Double value = null;

		final JsonElement element = jsonObject.get(propertyName);
		if (element != null && !element.isJsonNull()) {
			value = element.getAsDouble();
		}

		return value;
	}
	
	/**
	 * 
	 * @param propertyName
	 *            {@link String}
	 * @return property value as {@link BigDecimal}
	 */
	public BigDecimal getPropertyAsBigDecimal(String propertyName) {
		BigDecimal value = null;

		final JsonElement element = jsonObject.get(propertyName);
		if (element != null && !element.isJsonNull()) {
			value = element.getAsBigDecimal();
		}

		return value;
	}

	/**
	 * Transform array of strings into set of strings with preserved order
	 * 
	 * @param propertyName
	 *            {@link String}
	 * @return set of {@link String} elements by property name
	 */
	public Set<String> getPropertyAsStringSet(String propertyName) {
		final Set<String> set;

		final JsonElement element = jsonObject.get(propertyName);
		if (element != null && element.isJsonArray()) {
			set = new LinkedHashSet<>();
			final JsonArray jsonArray = element.getAsJsonArray();
			jsonArray.forEach(t -> set.add(t.getAsString()));
		} else {
			set = null;
		}

		return set;
	}

	public String[] getPropertyAsStringArray(String propertyName) {
		final JsonElement element = jsonObject.get(propertyName);
		if (element != null && element.isJsonArray()) {
			final JsonArray jsonArray = element.getAsJsonArray();
			List<String> elements = new ArrayList<>(jsonArray.size());
			jsonArray.forEach(e -> {
				if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
					elements.add(e.getAsString());
				}
			});
			return elements.toArray(new String[elements.size()]);
		} else {
			return null;
		}
	}

	public int[] getPropertyAsIntegerArray(String propertyName) {
		final int[] intArray;

		final JsonElement element = jsonObject.get(propertyName);
		if (element != null && element.isJsonArray()) {
			final JsonArray jsonArray = element.getAsJsonArray();
			intArray = new int[jsonArray.size()];
			IntStream.range(0, jsonArray.size()).forEach(i -> intArray[i] = jsonArray.get(i).getAsInt());
		} else {
			intArray = null;
		}

		return intArray;
	}

	public Integer[] getPropertyAsIntegerObjectArray(String propertyName) {
		final Integer[] intArray;

		final JsonElement element = jsonObject.get(propertyName);
		if (element != null && element.isJsonArray()) {
			final JsonArray jsonArray = element.getAsJsonArray();
			intArray = new Integer[jsonArray.size()];
			IntStream.range(0, jsonArray.size()).forEach(
					i -> intArray[i] = jsonArray.get(i).isJsonPrimitive() ? jsonArray.get(i).getAsInt() : null);
		} else {
			intArray = null;
		}

		return intArray;
	}

	public Long[] getPropertyAsLongObjectArray(String propertyName) {
		final Long[] longArray;

		final JsonElement element = jsonObject.get(propertyName);
		if (element != null && element.isJsonArray()) {
			final JsonArray jsonArray = element.getAsJsonArray();
			longArray = new Long[jsonArray.size()];
			IntStream.range(0, jsonArray.size()).forEach(
					i -> longArray[i] = jsonArray.get(i).isJsonPrimitive() ? jsonArray.get(i).getAsLong() : null);
		} else {
			longArray = null;
		}

		return longArray;
	}

	public long[] getPropertyAsLongArray(String propertyName) {
		final long[] longArray;

		final JsonElement element = jsonObject.get(propertyName);
		if (element != null && element.isJsonArray()) {
			final JsonArray jsonArray = element.getAsJsonArray();
			longArray = new long[jsonArray.size()];
			IntStream.range(0, jsonArray.size()).forEach(i -> longArray[i] = jsonArray.get(i).getAsLong());
		} else {
			longArray = null;
		}

		return longArray;
	}

	/**
	 * Adds elements as {@link JsonArray}
	 * 
	 * @param arrayPropertyName
	 *            {@link String}
	 * @param elements
	 *            {@link Set} of {@link String} elements to be added
	 */
	public void setArray(String arrayPropertyName, Set<String> elements) {
		final JsonArray elementArray = new JsonArray();
		if (elements != null) {
			elements.forEach(elementArray::add);
		}
		jsonObject.add(arrayPropertyName, elementArray);
	}

	public void setArray(String arrayPropertyName, String... elements) {
		final JsonArray elementArray = new JsonArray();
		if (elements != null) {
			Arrays.stream(elements).forEach(elementArray::add);
		}
		jsonObject.add(arrayPropertyName, elementArray);
	}
	
	public void setArray(String arrayPropertyName, Number... elements) {
		final JsonArray elementArray = new JsonArray();
		if (elements != null) {
			Arrays.stream(elements).forEach(elementArray::add);
		}
		jsonObject.add(arrayPropertyName, elementArray);
	}

	public JsonArray getArray(String propertyName) {
		final JsonElement element = jsonObject.get(propertyName);

		final JsonArray array;
		if (element != null && element.isJsonArray()) {
			array = element.getAsJsonArray();
		} else {
			array = null;
		}
		return array;
	}

	public void addElementToArray(String arrayProperty, JsonElement value) {
		JsonArray array = getArray(arrayProperty);
		if (array == null) {
			array = new JsonArray();
			jsonObject.add(arrayProperty, array);
		}
		array.add(value);
	}

	public void setElementOfArray(String propertyName, int index, JsonElement value, boolean isProcessExpiredDuel, boolean isRegisteredStepSwitch) {
		if (index < 0 || StringUtils.isBlank(propertyName)) {
			return;
		}
		JsonArray array = getArray(propertyName);
		LOGGER.debug("setElementOfArray 1 array {} ", array);
		if (array != null && array.size() > index) {
			LOGGER.debug("setElementOfArray 2 array {} ", array.get(index));
			array.set(index, value);
		} else {
			if (array == null && isProcessExpiredDuel) {
				LOGGER.debug("setElementOfArray 3");
				array = new JsonArray();
				array.add(value);
				jsonObject.add(propertyName, array);
			} else {
				LOGGER.debug("setElementOfArray 4");
				if (array == null) {
					LOGGER.debug("setElementOfArray 5");
					array = new JsonArray();
					jsonObject.add(propertyName, array);
					LOGGER.debug("setElementOfArray 5.5 {} ", jsonObject.getAsJsonArray(propertyName));
				}
				LOGGER.debug("setElementOfArray 6");
				for (int i = array.size(); i < index; i++) {
					array.add(JsonNull.INSTANCE);
					LOGGER.debug("setElementOfArray 7");
				}
				array.add(value);
			}
		}
	}

	public void setProperty(String propertyName, Object value) {
		if (value instanceof String) {
			setProperty(propertyName, (String) value);
		} else if (value instanceof Boolean) {
			setProperty(propertyName, (Boolean) value);
		} else if (value instanceof JsonElement) {
			setProperty(propertyName, (JsonElement) value);
		} else if (value instanceof ZonedDateTime) {
			setProperty(propertyName, (ZonedDateTime) value);
		} else if (value instanceof Number) {
			setProperty(propertyName, (Number) value);
		} else {
			throw new UnsupportedOperationException("Unknown value type: " + (value == null ? null : value.getClass()));
		}
	}

	/**
	 * Add {@link String} property
	 * 
	 * @param propertyName
	 *            {@link String}
	 * @param value
	 *            {@link String}
	 */
	public void setProperty(String propertyName, String value) {
		jsonObject.addProperty(propertyName, value);
	}

	/**
	 * Add {@link JsonElement} property
	 * 
	 * @param propertyName
	 *            {@link String}
	 * @param value
	 *            {@link JsonElement}
	 */
	public void setProperty(String propertyName, JsonElement value) {
		jsonObject.add(propertyName, value);
	}

	/**
	 * Add {@link Boolean} property
	 * 
	 * @param propertyName
	 *            {@link String}
	 * @param value
	 *            {@link Boolean}
	 */
	public void setProperty(String propertyName, Boolean value) {
		jsonObject.addProperty(propertyName, value);
	}

	/**
	 * Add {@link Number} property
	 * 
	 * @param propertyName
	 *            {@link String}
	 * @param value
	 *            {@link Number}
	 */
	public void setProperty(String propertyName, Number value) {
		jsonObject.addProperty(propertyName, value);
	}

	/**
	 * Add {@link ZonedDateTime} property
	 * 
	 * @param propertyName
	 *            {@link String}
	 * @param value
	 *            {@link ZonedDateTime}
	 */
	public void setProperty(String propertyName, ZonedDateTime value) {
		setProperty(propertyName, DateTimeUtil.formatISODateTime(value));
	}

	@Override
	public String toString() {
		return "BaseJsonObject [jsonObject=" + getAsString() + "]";
	}

}
