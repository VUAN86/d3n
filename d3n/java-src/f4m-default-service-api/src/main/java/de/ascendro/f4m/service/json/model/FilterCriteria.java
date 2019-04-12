package de.ascendro.f4m.service.json.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;

/** Structure for holding filtering criteria. */
public class FilterCriteria {

	/** Maximum number of results to be returned. */
	private int limit;

	/** Offset of returned results (results returned starting the given offset). */
	private long offset;

	/** Order by specifications, applied sequentially. */
	private List<OrderBy> orderBy;

	/** Search criteria specification, containing search field - search value pairs. */
	private LinkedHashMap<String, String> searchBy;

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public List<OrderBy> getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(List<OrderBy> orderBy) {
		this.orderBy = orderBy;
	}

	public void addOrderBy(OrderBy orderBy) {
		if (this.orderBy == null) {
			this.orderBy = new ArrayList<>();
		}
		this.orderBy.add(orderBy);
	}
	
	public LinkedHashMap<String, String> getSearchBy() {
		return searchBy;
	}

	public void setSearchBy(LinkedHashMap<String, String> searchBy) {
		this.searchBy = searchBy;
	}

	public void addSearchBy(String field, String value) {
		if (searchBy == null) {
			searchBy = new LinkedHashMap<>();
		}
		searchBy.put(field, value);
	}
	
	public String getSearchBy(String field) {
		return searchBy == null ? null : searchBy.get(field);
	}
	
	public boolean validateFilterCriteria(int maxListLimit) {
		if (limit > maxListLimit) {
			throw new F4MValidationFailedException(String.format("List limit exceeded (max: %s, specified: %s)", maxListLimit, limit));
		} else if (limit < 0) {
			throw new F4MValidationFailedException(String.format("List limit must be positive (specified: %s)", limit));
		} else if (offset < 0) {
			throw new F4MValidationFailedException(String.format("List offset must be positive (specified: %s)", offset));
		} else {
			return true;
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("FilterCriteria [limit=");
		builder.append(limit);
		builder.append(", offset=");
		builder.append(offset);
		builder.append(", orderBy=");
		builder.append(orderBy);
		builder.append(", searchBy=");
		builder.append(searchBy);
		builder.append("]");
		return builder.toString();
	}
	
}
