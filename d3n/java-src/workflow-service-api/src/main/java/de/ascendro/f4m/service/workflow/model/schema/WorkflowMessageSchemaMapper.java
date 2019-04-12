package de.ascendro.f4m.service.workflow.model.schema;

import de.ascendro.f4m.service.json.model.type.DefaultMessageSchemaMapper;

public class WorkflowMessageSchemaMapper extends DefaultMessageSchemaMapper {
	private static final long serialVersionUID = -4575812985735172610L;

	private static final String WORKFLOW_SCHEMA_PATH = "workflow.json";

	@Override
	protected void init() {
		this.register(WorkflowMessageSchemaMapper.class, "workflow", WORKFLOW_SCHEMA_PATH);
		super.init();
	}
}
