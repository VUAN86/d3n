package de.ascendro.f4m.service.json.model;

/** Order by criteria, specifying field and sort direction. */
public class OrderBy {

	/** Order direction enumeration. */
	public static enum Direction {
		asc, desc
	}

	/** Field to be ordered. */
	private String field;
	
	/** Ordering direction. */
	private Direction direction;

	public OrderBy() {
	}
	
	public OrderBy(String field) {
		this(field, Direction.asc);
	}
	
	public OrderBy(String field, Direction direction) {
		this.field = field;
		this.direction = direction;
	}
	
	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public Direction getDirection() {
		return direction;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

}
