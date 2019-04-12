package de.ascendro.f4m.service.workflow.server;

import de.ascendro.f4m.service.exception.validation.F4MValidationFailedException;
import de.ascendro.f4m.service.json.handler.DefaultJsonMessageHandler;
import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.JsonMessage;
import de.ascendro.f4m.service.json.model.JsonMessageContent;
import de.ascendro.f4m.service.workflow.WorkflowMessageTypes;
import de.ascendro.f4m.service.workflow.model.abort.AbortRequest;
import de.ascendro.f4m.service.workflow.model.perform.PerformRequest;
import de.ascendro.f4m.service.workflow.model.perform.PerformResponse;
import de.ascendro.f4m.service.workflow.model.start.StartRequest;
import de.ascendro.f4m.service.workflow.model.start.StartResponse;
import de.ascendro.f4m.service.workflow.model.state.StateRequest;
import de.ascendro.f4m.service.workflow.model.state.StateResponse;
import de.ascendro.f4m.service.workflow.utils.ActionResult;
import de.ascendro.f4m.service.workflow.utils.WorkflowWrapper;

public class WorkflowServiceServerMessageHandler extends DefaultJsonMessageHandler {

	private final WorkflowWrapper workflow;

	public WorkflowServiceServerMessageHandler(WorkflowWrapper workflow) {
		this.workflow = workflow;
	}

	@Override
	public JsonMessageContent onUserMessage(JsonMessage<? extends JsonMessageContent> message) {
		final WorkflowMessageTypes type = message.getType(WorkflowMessageTypes.class);
		if (type != null) {
			switch (type) {
			case START:
				return onStart((StartRequest) message.getContent());
			case PERFORM:
				return onPerform((PerformRequest) message.getContent());
			case STATE:
				return onState((StateRequest) message.getContent());
			case ABORT:
				return onAbort((AbortRequest) message.getContent());
			default:
				break;
			}
		}
		throw new F4MValidationFailedException("Unsupported message type[" + message.getTypeName() + "]");
	}

	protected JsonMessageContent onStart(StartRequest message) {
		ActionResult result = workflow.startProcess(message.getUserId(), message.getTenantId(), message.getTaskType(), 
				message.getTaskId(), message.getDescription(), message.getParameters());
		return new StartResponse(result.getAvailableStates(), result.isProcessFinished());
	}

	protected JsonMessageContent onPerform(PerformRequest message) {
		ActionResult result = workflow.doAction(message.getUserId(), message.getTenantId(), message.getTaskId(), 
				message.getActionType(), message.getParameters());
		return new PerformResponse(result.getPreviousState(), result.getAvailableStates(), result.isProcessFinished());
	}

	protected JsonMessageContent onState(StateRequest message) {
		ActionResult result = workflow.getState(message.getTaskId());
		return new StateResponse(result.getAvailableStates(), result.isProcessFinished());
	}

	protected JsonMessageContent onAbort(AbortRequest message) {
		workflow.abort(message.getTaskId());
		return new EmptyJsonMessageContent();
	}

}
