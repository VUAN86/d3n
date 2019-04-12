package de.ascendro.f4m.service.workflow.utils;

public class TaskInfo {

	private long id;
	private String name;

	public TaskInfo(long id, String name) {
		this.id = id;
		this.name = name;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

}
