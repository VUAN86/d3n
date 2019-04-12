package de.ascendro.f4m.service.payment.rest.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Generic class for list responses.
 * @param <T>
 */
public class ListResponseRest<T> {
	@JsonProperty("Limit")
	protected int limit;
	@JsonProperty("Offset")
	protected int offset;
	@JsonProperty("TotalCount")
	protected long total;
	@JsonProperty("Data")
	protected List<T> data;

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public List<T> getData() {
		return data;
	}

	public void setData(List<T> data) {
		this.data = data;
	}

}
