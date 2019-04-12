package de.ascendro.f4m.service.workflow.utils;

import javax.transaction.Synchronization;

import org.jbpm.workflow.instance.node.WorkItemNodeInstance;
import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.process.ProcessNodeLeftEvent;
import org.kie.api.event.process.ProcessNodeTriggeredEvent;
import org.kie.api.event.process.ProcessStartedEvent;
import org.kie.api.event.process.ProcessVariableChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.BitronixTransactionManager;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.workflow.model.UserTaskTriggeredEvent;

/**
 * Event listener for sending events when user nodes are triggered.
 */
public class EventSendingProcessEventListener implements ProcessEventListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(EventSendingProcessEventListener.class);
	
	private static final String WORKFLOW_TOPIC = "workflow/*";
	
	private JsonMessageUtil jsonUtil;
	private EventServiceClient eventServiceClient;
	private BitronixTransactionManager transactionManager;
	
	public EventSendingProcessEventListener(BitronixTransactionManager transactionManager, 
			JsonMessageUtil jsonUtil, EventServiceClient eventServiceClient) {
		this.jsonUtil = jsonUtil;
		this.eventServiceClient = eventServiceClient;
		this.transactionManager = transactionManager;
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
		// Nothing to do here
	}

	@Override
	public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
		if (event.getNodeInstance() instanceof WorkItemNodeInstance) {
			sendEvent(event.getProcessInstance().getProcessId(), getTaskId(event), event.getNodeInstance().getNodeName());
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

	/** Get the task ID from process instance. */
	private String getTaskId(ProcessNodeTriggeredEvent event) {
		return (String) event.getNodeInstance().getVariable(WorkflowWrapper.PARAM_TASK_ID);
	}
	
	/** Publish event on node triggered */
	private void sendEvent(String taskType, String taskId, String nodeTriggered) {
		BitronixTransaction transaction = transactionManager.getCurrentTransaction();
		if (transaction != null) {
			try {
				transaction.registerSynchronization(new Synchronization() {
					@Override
					public void beforeCompletion() {
						// Nothing to do here
					}
					
					@Override
					public void afterCompletion(int arg0) {
						doSendEvent(taskType, taskId, nodeTriggered);
					}
				});			
			} catch (Exception e) {
				LOGGER.error("Could not register synchronization. Sending event immediately.", e);
				transaction = null;
			}
		}
		if (transaction == null) {
			doSendEvent(taskType, taskId, nodeTriggered);
		}
	}

	private void doSendEvent(String taskType, String taskId, String nodeTriggered) {
		JsonElement event = jsonUtil.toJsonElement(new UserTaskTriggeredEvent(taskType, taskId, nodeTriggered));
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Publishing event " + nodeTriggered + " with ID " + taskId + " and taskType " + taskType + " to topic " + WORKFLOW_TOPIC);
		}
		eventServiceClient.publish(WORKFLOW_TOPIC, event);
	}

}
