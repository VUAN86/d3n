package de.ascendro.f4m.service.workflow;

import de.ascendro.f4m.service.json.model.type.MessageType;

/**
 * Voucher Service supported messages
 */
public enum WorkflowMessageTypes implements MessageType {
	
	START, START_RESPONSE,
	PERFORM, PERFORM_RESPONSE,
	STATE, STATE_RESPONSE,
	ABORT, ABORT_RESPONSE;

	public static final String SERVICE_NAME = "workflow";

	@Override
	public String getShortName() {
        return convertEnumNameToMessageShortName(name());
	}

	@Override
	public String getNamespace() {
		return SERVICE_NAME;
	}
}