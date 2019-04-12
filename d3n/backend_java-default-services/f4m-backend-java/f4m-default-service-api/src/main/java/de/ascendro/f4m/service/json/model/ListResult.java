package de.ascendro.f4m.service.json.model;

import java.util.ArrayList;
import java.util.List;

/** Structure for holding listing results. */
public class ListResult<T> {

	/** Result items. */
	private List<T> items;
	
	/** Offset in overall result list. */
	private long offset;
	
	/** Number of requested items from the overall result list. */
	private int limit;
	
	/** Total number of results. */
	private long total;

	public ListResult(int limit, long offset) {
		this(limit, offset, 0);
	}
	
	public ListResult(int limit, long offset, long total) {
		this(limit, offset, total, new ArrayList<>());
	}
	
	public ListResult(int limit, long offset, long total, List<T> items) {
		this.limit = limit;
		this.offset = offset;
		this.total = total;
		this.items = items;
	}
	
	public List<T> getItems() {
		return items;
	}

	public void setItems(List<T> items) {
		this.items = items;
	}

	public void addItem(T item) {
		if (items == null) {
			items = new ArrayList<>();
		}
		items.add(item);
	}
	
	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public int getSize() {
		return items == null ? 0 : items.size();
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(getClass().getSimpleName()).append(" [");
		builder.append("limit=").append(limit);
		builder.append(", offset=").append(offset);
		builder.append(", total=").append(total);
		builder.append(", items=").append(items);
		builder.append("]");
		return builder.toString();
	}

}
