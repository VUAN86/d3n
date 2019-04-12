package de.ascendro.f4m.service.workflow.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;

public class UnpublishingWorkflowTest extends WorkflowTestBase {

	private static final String PROCESS_ID = "UnpublishingWorkflow";

	@Override
	protected List<Resource> getResourceList() {
		return Arrays.asList(
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/UnpublishingWorkflow.bpmn"),
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/ArchivingWorkflow.bpmn"),
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/ErrorWorkflow.bpmn"));
	}

	@Test
	public void testSuccessfulUnpublishingNoArchive() {
		Map<String, Object> params = new HashMap<>();
		params.put(PARAM_ARCHIVE, VALUE_FALSE);
		params.put(PARAM_ITEM_STATUS, VALUE_PUBLISHED);
		
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, params), 
				false, null, asList(ACTION_UNPUBLISH), asList(ACTION_UNPUBLISH));
		assertResult(workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_UNPUBLISH, singletonMap(PARAM_RESULT, VALUE_SUCCESS)),
				true, ACTION_UNPUBLISH, asList(ACTION_SET_STATUS_UNPUBLISHED), emptyList());
	}
	
	@Test
	public void testSuccessfulUnpublishingArchive() {
		Map<String, Object> params = new HashMap<>();
		params.put(PARAM_ARCHIVE, VALUE_TRUE);
		params.put(PARAM_ITEM_STATUS, VALUE_PUBLISHED);
		
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, params), 
				false, null, asList(ACTION_UNPUBLISH), asList(ACTION_UNPUBLISH));
		assertResult(workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_UNPUBLISH, singletonMap(PARAM_RESULT, VALUE_SUCCESS)),
				false, ACTION_UNPUBLISH, asList(ACTION_SET_STATUS_UNPUBLISHED, ACTION_ARCHIVE), asList(ACTION_ARCHIVE));
		assertResult(workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_ARCHIVE, singletonMap(PARAM_RESULT, VALUE_SUCCESS)),
				true, ACTION_ARCHIVE, asList(ACTION_SET_STATUS_ARCHIVED), emptyList());
	}
	
	@Test
	public void testSuccessfulUnpublishingArchiveFailed() {
		Map<String, Object> params = new HashMap<>();
		params.put(PARAM_ARCHIVE, VALUE_TRUE);
		params.put(PARAM_ITEM_STATUS, VALUE_PUBLISHED);
		
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, params), 
				false, null, asList(ACTION_UNPUBLISH), asList(ACTION_UNPUBLISH));
		assertResult(workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_UNPUBLISH, singletonMap(PARAM_RESULT, VALUE_SUCCESS)),
				false, ACTION_UNPUBLISH, asList(ACTION_SET_STATUS_UNPUBLISHED, ACTION_ARCHIVE), asList(ACTION_ARCHIVE));
		assertResult(workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_ARCHIVE, singletonMap(PARAM_RESULT, VALUE_FAILURE)),
				true, ACTION_ARCHIVE, asList(ACTION_SEND_EMAIL_ADMIN), emptyList());
	}
	
	
	@Test
	public void testUnpublishingFailed() {
		Map<String, Object> params = new HashMap<>();
		params.put(PARAM_ARCHIVE, VALUE_FALSE);
		params.put(PARAM_ITEM_STATUS, VALUE_PUBLISHED);
		
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, params), 
				false, null, asList(ACTION_UNPUBLISH), asList(ACTION_UNPUBLISH));
		assertResult(workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_UNPUBLISH, singletonMap(PARAM_RESULT, VALUE_FAILURE)),
				true, ACTION_UNPUBLISH, asList(ACTION_SEND_EMAIL_ADMIN), emptyList());
	}	

	@Test
	public void testStatusError() {
		Map<String, Object> params = new HashMap<>();
		params.put(PARAM_ARCHIVE, VALUE_FALSE);
		params.put(PARAM_ITEM_STATUS, VALUE_UNPUBLISHED);
		
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, params), 
				true, null, asList(ACTION_SEND_EMAIL_ADMIN), emptyList());
	}

}
