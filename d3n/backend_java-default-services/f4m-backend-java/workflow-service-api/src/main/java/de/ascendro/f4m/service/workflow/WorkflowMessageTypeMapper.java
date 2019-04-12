package de.ascendro.f4m.service.workflow;

import com.google.gson.reflect.TypeToken;

import de.ascendro.f4m.service.json.model.EmptyJsonMessageContent;
import de.ascendro.f4m.service.json.model.type.JsonMessageTypeMapImpl;
import de.ascendro.f4m.service.workflow.model.abort.AbortRequest;
import de.ascendro.f4m.service.workflow.model.perform.PerformRequest;
import de.ascendro.f4m.service.workflow.model.perform.PerformResponse;
import de.ascendro.f4m.service.workflow.model.start.StartRequest;
import de.ascendro.f4m.service.workflow.model.start.StartResponse;
import de.ascendro.f4m.service.workflow.model.state.StateRequest;
import de.ascendro.f4m.service.workflow.model.state.StateResponse;

public class WorkflowMessageTypeMapper extends JsonMessageTypeMapImpl {

	private static final long serialVersionUID = -9124628350006408747L;

	public WorkflowMessageTypeMapper() {
		init();
	}

	protected void init() {
		
		this.register(WorkflowMessageTypes.PERFORM.getMessageName(), new TypeToken<PerformRequest>() {}.getType());
		this.register(WorkflowMessageTypes.PERFORM_RESPONSE.getMessageName(),new TypeToken<PerformResponse>() {}.getType());

		this.register(WorkflowMessageTypes.START.getMessageName(),new TypeToken<StartRequest>() {}.getType());
		this.register(WorkflowMessageTypes.START_RESPONSE.getMessageName(),new TypeToken<StartResponse>() {}.getType());

		this.register(WorkflowMessageTypes.STATE.getMessageName(),new TypeToken<StateRequest>() {}.getType());
		this.register(WorkflowMessageTypes.STATE_RESPONSE.getMessageName(),new TypeToken<StateResponse>() {}.getType());
		
		this.register(WorkflowMessageTypes.ABORT.getMessageName(),new TypeToken<AbortRequest>() {}.getType());
		this.register(WorkflowMessageTypes.ABORT_RESPONSE.getMessageName(),new TypeToken<EmptyJsonMessageContent>() {}.getType());

	}
}
