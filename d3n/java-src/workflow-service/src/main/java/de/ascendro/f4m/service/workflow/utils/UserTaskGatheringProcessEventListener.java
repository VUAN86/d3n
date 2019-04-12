package de.ascendro.f4m.service.workflow.utils;

import java.util.ArrayList;
import java.util.List;

import org.jbpm.workflow.instance.node.HumanTaskNodeInstance;
import org.jbpm.workflow.instance.node.WorkItemNodeInstance;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.process.ProcessNodeLeftEvent;
import org.kie.api.event.process.ProcessNodeTriggeredEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.event.process.ProcessVariableChangedEvent;

public class UserTaskGatheringProcessEventListener implements ProcessEventListener {

	private List<String> userTasksTriggered = new ArrayList<>(5);
	private List<String> tasksTriggered = new ArrayList<>(5);
	
	private boolean processFinished;
	
	public List<String> getTasksTriggered() {
		return tasksTriggered;
	}

	public List<String> getUserTasksTriggered() {
		return userTasksTriggered;
	}
	
	public boolean isProcessFinished() {
		return processFinished;
	}
	
	@Override
	public void beforeProcessStarted(ProcessStartedEvent event) {
		// Nothing to do here
	}

	@Override
	public void afterProcessStarted(ProcessStartedEvent event) {
		// Nothing to do here
	}

	@Override
	public void beforeProcessCompleted(ProcessCompletedEvent event) {
		// Nothing to do here
	}

	@Override
	public void afterProcessCompleted(ProcessCompletedEvent event) {
		processFinished = event.getProcessInstance().getParentProcessInstanceId() == 0;
	}

	@Override
	public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
		if (event.getNodeInstance() instanceof WorkItemNodeInstance) {
			tasksTriggered.add(event.getNodeInstance().getNodeName());
			if (event.getNodeInstance() instanceof HumanTaskNodeInstance) {
				userTasksTriggered.add(event.getNodeInstance().getNodeName());
			}
		}
	}

	@Override
	public void afterNodeTriggered(ProcessNodeTriggeredEvent event) {
		// Nothing to do here
	}

	@Override
	public void beforeNodeLeft(ProcessNodeLeftEvent event) {
		// Nothing to do here
	}

	@Override
	public void afterNodeLeft(ProcessNodeLeftEvent event) {
		// Nothing to do here
	}

	@Override
	public void beforeVariableChanged(ProcessVariableChangedEvent event) {
		// Nothing to do here
	}

	@Override
	public void afterVariableChanged(ProcessVariableChangedEvent event) {
		// Nothing to do here
	}

}
