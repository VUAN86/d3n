package de.ascendro.f4m.service.workflow.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;

public class PublishingWorkflowTest extends WorkflowTestBase {

	private static final String PROCESS_ID = "PublishingWorkflow";

	@Override
	protected List<Resource> getResourceList() {
		return Arrays.asList(
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/PublishingWorkflow.bpmn"),
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/ErrorWorkflow.bpmn"));
	}

	@Test
	public void testSuccessfulPublishing() {
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, null), 
				false, null, asList(ACTION_PUBLISH), asList(ACTION_PUBLISH));
		assertResult(workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_PUBLISH, Collections.singletonMap(PARAM_RESULT, VALUE_SUCCESS)),
				true, ACTION_PUBLISH, asList(ACTION_SET_STATUS_PUBLISHED), emptyList());
	}

	@Test
	public void testPublishingError() {
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, null), 
				false, null, asList(ACTION_PUBLISH), asList(ACTION_PUBLISH));
		assertResult(workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_PUBLISH, Collections.singletonMap(PARAM_RESULT, VALUE_FAILURE)),
				true, ACTION_PUBLISH, asList(ACTION_SEND_EMAIL_ADMIN), emptyList());
	}

}
