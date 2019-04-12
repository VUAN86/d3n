package de.ascendro.f4m.service.workflow.utils;

import static java.util.Arrays.asList;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;

import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;

public class RepublishingWorkflowTest extends WorkflowTestBase {

	private static final String PROCESS_ID = "RepublishingWorkflow";

	@Override
	protected List<Resource> getResourceList() {
		return Arrays.asList(
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/RepublishingWorkflow.bpmn"),
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/RejectionWorkflow.bpmn"),
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/PublishingWorkflow.bpmn"));
	}

	@Test
	public void testRepublishing() {
		prepareProfileRoles(USER_ID, ROLE_INTERNAL);
		prepareProfileRoles(USER_ID_2, ROLE_INTERNAL);
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, null), 
				false, null, asList(ACTION_INTERNAL_REVIEW), asList(ACTION_INTERNAL_REVIEW));

		// Author may not review his own question
		try {
			workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED));
			fail("Author may not review his own content");
		} catch (F4MInsufficientRightsException e) {
			// expected
		}

		// Have to be internal
		try {
			prepareProfileRoles("wrongRoles", ROLE_EXTERNAL);
			workflowWrapper.doAction("wrongRoles", TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED));
			fail("User have to have appropriate roles");
		} catch (F4MInsufficientRightsException e) {
			// expected
		}
		
		// First, test reject scenario
		assertResult(workflowWrapper.doAction(USER_ID_2, TENANT_ID, TASK_ID, ACTION_INTERNAL_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_REJECTED)),
				false, ACTION_INTERNAL_REVIEW, asList(ACTION_SEND_EMAIL_AUTHOR, ACTION_EDIT_QUESTION), asList(ACTION_EDIT_QUESTION));
		
		// Edit question
		try {
			workflowWrapper.doAction(USER_ID_2, TENANT_ID, TASK_ID, ACTION_EDIT_QUESTION, null);
			fail("Edit allowed only for user who created question");
		} catch (F4MInsufficientRightsException e) {
			// expected
		}

		assertResult(workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_EDIT_QUESTION, Collections.singletonMap(PARAM_EDIT_OUTCOME, VALUE_EDITED)),
				false, ACTION_EDIT_QUESTION, asList(ACTION_INTERNAL_REVIEW), asList(ACTION_INTERNAL_REVIEW));

		// Then - approved scenario
		assertResult(workflowWrapper.doAction(USER_ID_2, TENANT_ID, TASK_ID, ACTION_INTERNAL_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED)),
				false, ACTION_INTERNAL_REVIEW, asList(ACTION_PUBLISH), asList(ACTION_PUBLISH));
	}

}
