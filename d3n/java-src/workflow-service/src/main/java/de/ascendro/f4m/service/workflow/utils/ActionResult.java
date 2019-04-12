package de.ascendro.f4m.service.workflow.utils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ActionResult {

	private String previousState;
	private List<String> newStatesTriggered;
	private Set<String> availableStates;
	private boolean processFinished;

	public ActionResult(Set<String> availableStates, boolean processFinished) {
		this(null, null, availableStates, processFinished);
	}
	
	public ActionResult(List<String> newStatesTriggered, Set<String> availableStates, boolean processFinished) {
		this(null, newStatesTriggered, availableStates, processFinished);
	}
	
	public ActionResult(String previousState, List<String> newStatesTriggered, Set<String> availableStates, boolean processFinished) {
		this.previousState = previousState;
		this.newStatesTriggered = newStatesTriggered;
		this.processFinished = processFinished;
		this.availableStates = availableStates;
	}
	
	public String getPreviousState() {
		return previousState;
	}
	
	public List<String> getNewStatesTriggered() {
		return newStatesTriggered == null ? Collections.emptyList() : newStatesTriggered;
	}
	
	public Set<String> getAvailableStates() {
		return availableStates;
	}
	
	public boolean isProcessFinished() {
		return processFinished;
	}
	
}
