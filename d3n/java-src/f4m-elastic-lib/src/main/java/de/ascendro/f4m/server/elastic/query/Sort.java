package de.ascendro.f4m.server.elastic.query;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonObject;

import de.ascendro.f4m.service.json.model.JsonObjectWrapper;

/**
 * Class for creating elastic search sorting.
 */
public class Sort extends JsonObjectWrapper {

	private static final String PROPERTY_ORDER = "order";
	private static final String PROPERTY_NESTED_PATH = "nestedPath";
	
	private Sort() {
	}
	
	public static Sort newSort(String propertyName) {
		return newSort(propertyName, SortDirection.asc);
	}
	
	public static Sort newSort(String propertyName, SortDirection direction) {
		return newSort(null, propertyName, direction);
	}

	public static Sort newSort(String nestedPath, String propertyName) {
		Sort sort = new Sort();
		sort.sort(nestedPath, propertyName, SortDirection.asc);
		return sort;
	}

	public static Sort newSort(String nestedPath, String propertyName, SortDirection direction) {
		Sort sort = new Sort();
		sort.sort(nestedPath, propertyName, direction);
		return sort;
	}

	public Sort sort(String propertyName) {
		return sort(propertyName, SortDirection.asc);
	}
	
	public Sort sort(String propertyName, SortDirection direction) {
		return sort(null, propertyName, direction);
	}
	
	public Sort sort(String nestedPath, String propertyName) {
		return sort(nestedPath, propertyName, SortDirection.asc);
	}
	
	public Sort sort(String nestedPath, String propertyName, SortDirection direction) {
		JsonObject child = new JsonObject();
		if (StringUtils.isNotBlank(nestedPath)) {
			child.addProperty(PROPERTY_NESTED_PATH, nestedPath);
		}
		child.addProperty(PROPERTY_ORDER, direction.name());
		setProperty(propertyName, child);
		return this;
	}
	
}
