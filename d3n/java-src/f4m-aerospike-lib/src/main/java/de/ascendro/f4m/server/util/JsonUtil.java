package de.ascendro.f4m.server.util;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.service.di.GsonProvider;
import de.ascendro.f4m.service.json.model.JsonObjectWrapper;
import de.ascendro.f4m.service.json.model.OrderBy;
import de.ascendro.f4m.service.json.model.OrderBy.Direction;

public class JsonUtil {

	private static final boolean NULLS_FIRST = true;

	private Gson gson;

	public JsonUtil() {
		gson = new GsonProvider().get();
	}

	public <T> T fromJson(String jsonString, Class<T> jsonStringClass) {
		return gson.fromJson(jsonString, jsonStringClass);
	}
	
	public <T> T fromJson(String jsonString, Type type) {
		return gson.fromJson(jsonString, type);
	}

	public <T> T fromJson(JsonElement jsonElement, Class<T> jsonStringClass) {
		return gson.<T> fromJson(jsonElement, jsonStringClass);
	}
	
	public String toJson(Object source) {
		return gson.toJson(source);
	}
	
	public JsonElement toJsonElement(Object source) {
		return gson.toJsonTree(source);
	}

	public <T> List<T> getEntityListFromJsonString(String contents, Type type) {
		List<T> list;
		if (StringUtils.isNotEmpty(contents)) {
			list = fromJson(contents, type);
		} else {
			list = new ArrayList<>(1);
		}
		return list;
	}

	public <T> String convertEntitiesToJsonString(List<T> list) {
		JsonArray array = new JsonArray();
		for (T listMessage : list) {
			array.add(gson.toJsonTree(listMessage));
		}
		return array.toString();
	}

	/**
	 * Sort the given json element list in given order.
	 * <code>null</code> values will be sorted first.
	 * @param list List to sort
	 * @param orderBy Order by specification
	 */
	public static void sort(List<? extends JsonElement> list, List<OrderBy> orderBy) {
		Collections.sort(list, (JsonElement p1, JsonElement p2) -> compare(p1, p2, orderBy));
	}

	private static int compare(JsonElement p1, JsonElement p2, List<OrderBy> orderBy) {
		if (CollectionUtils.isEmpty(orderBy)) {
			return compare(p1, p2, Direction.asc);
		} else if (p1.isJsonObject() && p2.isJsonObject()) {
			int cmp = 0;
			for (OrderBy order : orderBy) {
				cmp = compare(getElement(p1.getAsJsonObject(), order.getField()),
						getElement(p2.getAsJsonObject(), order.getField()), order.getDirection());
				if (cmp != 0) {
					return cmp;
				}
			}
			return cmp;
		} else {
			return compare(p1, p2, orderBy.iterator().next().getDirection());
		}
	}

	private static JsonElement getElement(JsonObject element, String field) {
		if (field == null) {
			return null;
		}
		String[] memberNames = field.split("\\.");
		JsonElement result = element;
		for (String memberName : memberNames) {
			if (result != null && result.isJsonObject()) {
				result = result.getAsJsonObject().get(memberName);
			} else {
				return null;
			}
		}
		return result;
	}

	private static int compare(JsonElement p1, JsonElement p2, Direction direction) {
		int result;
		if (isNull(p1) && isNull(p2)) {
			result = 0;
		} else if (isNull(p1)) {
			result = NULLS_FIRST ? -1 : 1;
		} else if (isNull(p2)) {
			result = NULLS_FIRST ? 1 : -1;
		} else {
			result = (p1.isJsonPrimitive() ? p1.getAsString() : p1.toString())
					.compareTo(p2.isJsonPrimitive() ? p2.getAsString() : p2.toString());
		}
		return result * (direction == Direction.asc ? 1 : -1);
	}

	private static boolean isNull(JsonElement element) {
		return element == null || element.isJsonNull();
	}

	/**
	 * Filter list of Json objects by given field - filter expressions.
	 * Allows filter by exact value or using wildcards (* and ?)
	 * @param list List to filter
	 * @param searchBy Filter criteria
	 * @return Filtered list
	 */
	public static List<JsonObject> filter(List<JsonObject> list, Map<String, String> searchBy) {
		if (MapUtils.isEmpty(searchBy)) {
			return list;
		}
		return list.stream().filter(o -> matches(o, searchBy)).collect(Collectors.toList());
	}

	private static boolean matches(JsonObject object, Map<String, String> searchBy) {
		for (Entry<String, String> filter : searchBy.entrySet()) {
			if (! matches(object, filter.getKey(), filter.getValue())) {
				return false;
			}
		}
		return true;
	}

	private static boolean matches(JsonObject object, String field, String filter) {
		if (filter == null) {
			return true;
		}
		JsonElement element = getElement(object, field);
		if (isNull(element)) {
			return false;
		}
		if (element.isJsonArray()) {
			for (JsonElement e : (JsonArray) element) {
				if (matches(getElementStringValue(e), filter)) {
					return true;
				}
			}
			return false;
		} else {
			return matches(getElementStringValue(element), filter);
		}
	}

	private static String getElementStringValue(JsonElement element) {
		return element.isJsonPrimitive() ? element.getAsString() : element.toString();		
	}
	
	private static boolean matches(String value, String filter) {
        final StringBuilder regex = new StringBuilder(filter.length() * 2 + 2);
        regex.append("(?i)^");
		for (final char c : filter.toCharArray()) {
			switch (c) {
			case '?':
				regex.append(".");
				break;
			case '*':
				regex.append(".*");
				break;
			default:
				regex.append(Pattern.quote(String.valueOf(c)));
				break;
			}
		}
        regex.append("$");
		Pattern pattern = Pattern.compile(regex.toString());
		return pattern.matcher(value).matches();
	}

	/**
	 * Extract parts of Json object
	 * @param object The object
	 * @param memberNames Parts to be extracted
	 * @return Object with only the selected parts extracted
	 */
	public static JsonObject extractParts(JsonObject object, String... memberNames) {
		if (memberNames.length == 0) {
			return object; // return full
		}
		
		// extract only necessary information
		JsonObject item = new JsonObject();
		for (String memberName : memberNames) {
			JsonElement value = object.get(memberName);
			if (value != null) {
				item.add(memberName, value);
			}
		}
		return item;
	}
	
	public <T extends JsonObjectWrapper> T toJsonObjectWrapper(String jsonString, Function<JsonObject, T> toWrapperFunc) {
		T result = null;
		if (jsonString != null) {
			JsonObject jsonObject = fromJson(jsonString, JsonObject.class);
			result = toWrapperFunc.apply(jsonObject);
		}
		return result;
	}
	
	public JsonArray toJsonArray(JsonElement element) {
		JsonArray array;
		if (element instanceof JsonArray) {
			array = (JsonArray) element;
		} else if (element instanceof JsonObject) {
			array = new JsonArray();
			array.add(element);
		} else {
			array = new JsonArray();
		}
		return array;
	}
	
	public static JsonObject deepCopy(JsonObject jsonObject) {
	    JsonObject result = new JsonObject();
	    for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
	        result.add(entry.getKey(), deepCopy(entry.getValue()));
	    }
	    return result;
	}

	public static JsonArray deepCopy(JsonArray jsonArray) {
	    JsonArray result = new JsonArray();
	    for (JsonElement e : jsonArray) {
	        result.add(deepCopy(e));
	    }
	    return result;
	}

	public static JsonElement deepCopy(JsonElement jsonElement) {
	    if (jsonElement.isJsonPrimitive() || jsonElement.isJsonNull()) {
	        return jsonElement;       // these are immutables anyway
	    } else if (jsonElement.isJsonObject()) {
	        return deepCopy(jsonElement.getAsJsonObject());
	    } else if (jsonElement.isJsonArray()) {
	        return deepCopy(jsonElement.getAsJsonArray());
	    } else {
	        throw new UnsupportedOperationException("Unsupported element: " + jsonElement);
	    }
	}
}
