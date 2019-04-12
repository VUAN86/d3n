package de.ascendro.f4m.service.workflow.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;

import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.integration.RetriedAssert;

public class TranslationWorkflowTest extends WorkflowTestBase {

	private static final String PROCESS_ID = "TranslationWorkflow";

	private static final String USER_ID_2 = "anotheruser";

	private static final int REJECTS_NEEDED_FOR_TOTAL_REJECT = 2;
	private static final int APPROVALS_NEEDED_FOR_TOTAL_APPROVE = 3;

	@Override
	protected List<Resource> getResourceList() {
		return Arrays.asList(
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/TranslationWorkflow.bpmn"),
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/ArchivingWorkflow.bpmn"),
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/RejectionWorkflow.bpmn"),
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/PublishingWorkflow.bpmn"),
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/ErrorWorkflow.bpmn"));
	}

	
	@Test
	public void testTranslationReview() {
		prepareProfileRoles(USER_ID, ROLE_COMMUNITY);
		
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_TRANSLATION_TYPE, VALUE_COMMUNITY)), 
				false, null, asList(ACTION_SET_STATUS_REVIEW, ACTION_COMMUNITY_REVIEW), asList(ACTION_COMMUNITY_REVIEW));
		
		// Author may not review his own question
		try {
			workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED));
			fail("Author may not review his own content");
		} catch (F4MInsufficientRightsException e) {
			// expected
		}

		// not enough positive reviews to approve
		for (int i = 0; i < APPROVALS_NEEDED_FOR_TOTAL_APPROVE - 3 ; i++) {
			prepareProfileRoles("user" + i, ROLE_COMMUNITY);
			assertResult(workflowWrapper.doAction("user" + i, TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED)),
					false, ACTION_COMMUNITY_REVIEW, asList(ACTION_COMMUNITY_REVIEW), asList(ACTION_COMMUNITY_REVIEW));
		}
		
		final String userNameInternal="user_internal";
		prepareProfileRoles(userNameInternal , ROLE_INTERNAL);
		assertResult(workflowWrapper.doAction(userNameInternal, TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED)),
				false, ACTION_COMMUNITY_REVIEW, asList(ACTION_COMMUNITY_REVIEW), asList(ACTION_COMMUNITY_REVIEW));
		
		// Same user may not review twice
		try {
			workflowWrapper.doAction(userNameInternal, TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED));
			fail("Same user may not review twice");
		} catch (F4MInsufficientRightsException e) {
			// expected
		}
		
		final String userNameExternal="user_external";
		prepareProfileRoles(userNameExternal, ROLE_EXTERNAL);
		assertResult(workflowWrapper.doAction(userNameExternal, TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED)),
				false, ACTION_COMMUNITY_REVIEW, asList(ACTION_COMMUNITY_REVIEW), asList(ACTION_COMMUNITY_REVIEW));
		
		// Last positive review moves forward
		final String userNameCommunity="user_community";
		prepareProfileRoles(userNameCommunity, ROLE_COMMUNITY);
		assertResult(workflowWrapper.doAction(userNameCommunity, TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED)),
				false, ACTION_COMMUNITY_REVIEW, asList(ACTION_SET_STATUS_APPROVED, ACTION_PUBLISH), asList(ACTION_PUBLISH));
		
		// Publish
		assertResult(workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_PUBLISH, Collections.singletonMap(PARAM_RESULT, VALUE_SUCCESS)),
				true, ACTION_PUBLISH, asList(ACTION_SET_STATUS_PUBLISHED), emptyList());
	}
	
	/**
	 * Community review reject.
	 */
	@Test
	public void testCommunityReject() {
		prepareProfileRoles(USER_ID, ROLE_COMMUNITY);
		
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_TRANSLATION_TYPE, VALUE_COMMUNITY)), 
				false, null, asList(ACTION_SET_STATUS_REVIEW, ACTION_COMMUNITY_REVIEW), asList(ACTION_COMMUNITY_REVIEW));
		
		// Not enough negative reviews to reject
		for (int i = 0; i < REJECTS_NEEDED_FOR_TOTAL_REJECT - 1 ; i++) {
			prepareProfileRoles("user" + i, ROLE_COMMUNITY);
			assertResult(workflowWrapper.doAction("user" + i, TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_REJECTED)),
					false, ACTION_COMMUNITY_REVIEW, asList(ACTION_COMMUNITY_REVIEW), asList(ACTION_COMMUNITY_REVIEW));
		}
		
		// Last negative review moves forward
		prepareProfileRoles("user3", ROLE_COMMUNITY);
		assertResult(workflowWrapper.doAction("user3", TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_REJECTED)),
				false, ACTION_COMMUNITY_REVIEW, asList(ACTION_SET_STATUS_REJECTED, ACTION_SEND_EMAIL_AUTHOR, ACTION_EDIT_QUESTION), asList(ACTION_EDIT_QUESTION));
		
		// Edit question
		try {
			workflowWrapper.doAction("user0", TENANT_ID, TASK_ID, ACTION_EDIT_QUESTION, null);
			fail("Edit allowed only for user who created question");
		} catch (F4MInsufficientRightsException e) {
			// expected
		}
		
		assertResult(workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_EDIT_QUESTION, Collections.singletonMap(PARAM_EDIT_OUTCOME, VALUE_EDITED)),
				false, ACTION_EDIT_QUESTION, asList(ACTION_SET_STATUS_REVIEW, ACTION_COMMUNITY_REVIEW), asList(ACTION_COMMUNITY_REVIEW));
		
		// Now, test timeout scenario
		setFireTimersImmediately(true);
		
		// Not enough negative reviews to reject
		for (int i = 0; i < REJECTS_NEEDED_FOR_TOTAL_REJECT - 1 ; i++) {
			assertResult(workflowWrapper.doAction("user" + i, TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_REJECTED)),
					false, ACTION_COMMUNITY_REVIEW, asList(ACTION_COMMUNITY_REVIEW), asList(ACTION_COMMUNITY_REVIEW));
		}
		assertResult(workflowWrapper.doAction("user3", TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_REJECTED)),
				false, ACTION_COMMUNITY_REVIEW, asList(ACTION_SET_STATUS_REJECTED, ACTION_SEND_EMAIL_AUTHOR, ACTION_EDIT_QUESTION), asList(ACTION_EDIT_QUESTION));
		
		// Now wait for timeout
		receivedEvents.clear();
		RetriedAssert.assertWithWait(() -> assertFalse(receivedEvents.isEmpty()));
		
		// In this case we go to archive
		assertThat(receivedEvents, contains(ACTION_ARCHIVE));
		RetriedAssert.assertWithWait(() -> assertResult(workflowWrapper.getState(TASK_ID), false, null, emptyList(), asList(ACTION_ARCHIVE)));
	}

	/**
	 * Community review timeout.
	 */
	@Test
	public void testCommunityTimeout() {
		prepareProfileRoles(USER_ID, ROLE_COMMUNITY);
		setFireTimersImmediately(true);
		
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_TRANSLATION_TYPE, VALUE_COMMUNITY)), 
				false, null, asList(ACTION_SET_STATUS_REVIEW, ACTION_COMMUNITY_REVIEW), asList(ACTION_COMMUNITY_REVIEW));
		
		// Wait for timer to trigger
		receivedEvents.clear();
		RetriedAssert.assertWithWait(() -> assertFalse(receivedEvents.isEmpty()));
		assertThat(receivedEvents, contains(ACTION_SET_PRIORITY_HIGH, ACTION_INTERNAL_REVIEW));
		
		// Wait for state to change
		RetriedAssert.assertWithWait(() -> assertResult(workflowWrapper.getState(TASK_ID), false, null, emptyList(), asList(ACTION_INTERNAL_REVIEW)));
	}
	
	@Test
	public void testInternalReview() {
		prepareProfileRoles(USER_ID, ROLE_INTERNAL);
		prepareProfileRoles(USER_ID_2, ROLE_INTERNAL);
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_TRANSLATION_TYPE, VALUE_INTERNAL)), 
				false, null, asList(ACTION_SET_STATUS_REVIEW, ACTION_INTERNAL_REVIEW), asList(ACTION_INTERNAL_REVIEW));

		// Author may not review his own question
		try {
			workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_INTERNAL_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED));
			fail("Author may not review his own content");
		} catch (F4MInsufficientRightsException e) {
			// expected
		}
		
		//community members cannot review internal tasks
		try {
			final String userNoRights = "user_no_rights";
			prepareProfileRoles(userNoRights, ROLE_COMMUNITY);
			workflowWrapper.doAction(userNoRights, TENANT_ID, TASK_ID, ACTION_INTERNAL_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED));
			fail("User have to have appropriate roles");
		} catch (F4MInsufficientRightsException e) {
			// expected
		}
		
		assertResult(workflowWrapper.doAction(USER_ID_2, TENANT_ID, TASK_ID, ACTION_INTERNAL_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED)),
				false, ACTION_INTERNAL_REVIEW, asList(ACTION_SET_STATUS_APPROVED, ACTION_PUBLISH), asList(ACTION_PUBLISH));
		assertResult(workflowWrapper.doAction(USER_ID_2, TENANT_ID, TASK_ID, ACTION_PUBLISH, Collections.singletonMap(PARAM_RESULT, VALUE_SUCCESS)),
				true, ACTION_PUBLISH, asList(ACTION_SET_STATUS_PUBLISHED), emptyList());
	}
	
	@Test
	public void testInternalReviewRejected() {
		prepareProfileRoles(USER_ID, ROLE_INTERNAL);
		prepareProfileRoles(USER_ID_2, ROLE_INTERNAL);
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_TRANSLATION_TYPE, VALUE_INTERNAL)), 
				false, null, asList(ACTION_SET_STATUS_REVIEW, ACTION_INTERNAL_REVIEW), asList(ACTION_INTERNAL_REVIEW));
		assertResult(workflowWrapper.doAction(USER_ID_2, TENANT_ID, TASK_ID, ACTION_INTERNAL_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_REJECTED)),
				false, ACTION_INTERNAL_REVIEW, asList(ACTION_SET_STATUS_REJECTED, ACTION_SEND_EMAIL_AUTHOR, ACTION_EDIT_QUESTION), asList(ACTION_EDIT_QUESTION));
		assertResult(workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_EDIT_QUESTION, Collections.singletonMap(PARAM_EDIT_OUTCOME, VALUE_EDITED)),
				false, ACTION_EDIT_QUESTION, asList(ACTION_SET_STATUS_REVIEW, ACTION_INTERNAL_REVIEW), asList(ACTION_INTERNAL_REVIEW));
	}
	
	@Test
	public void testExternalReview() {
		prepareProfileRoles(USER_ID, ROLE_EXTERNAL);
		prepareProfileRoles(USER_ID_2, ROLE_EXTERNAL);
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_TRANSLATION_TYPE, VALUE_EXTERNAL)), 
				false, null, asList(ACTION_SET_STATUS_REVIEW, ACTION_EXTERNAL_REVIEW), asList(ACTION_EXTERNAL_REVIEW));
		
		// Author may not review his own question
		try {
			workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_EXTERNAL_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED));
			fail("Author may not review his own content");
		} catch (F4MInsufficientRightsException e) {
			// expected
		}

		assertResult(workflowWrapper.doAction(USER_ID_2, TENANT_ID, TASK_ID, ACTION_EXTERNAL_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED)),
				false, ACTION_EXTERNAL_REVIEW, asList(ACTION_SET_STATUS_APPROVED, ACTION_PUBLISH), asList(ACTION_PUBLISH));
		assertResult(workflowWrapper.doAction(USER_ID_2, TENANT_ID, TASK_ID, ACTION_PUBLISH, Collections.singletonMap(PARAM_RESULT, VALUE_SUCCESS)),
				true, ACTION_PUBLISH, asList(ACTION_SET_STATUS_PUBLISHED), emptyList());
	}
	
	@Test
	public void testExternalReviewRejected() {
		prepareProfileRoles(USER_ID, ROLE_EXTERNAL);
		prepareProfileRoles(USER_ID_2, ROLE_EXTERNAL);
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_TRANSLATION_TYPE, VALUE_EXTERNAL)), 
				false, null, asList(ACTION_SET_STATUS_REVIEW, ACTION_EXTERNAL_REVIEW), asList(ACTION_EXTERNAL_REVIEW));
		assertResult(workflowWrapper.doAction(USER_ID_2, TENANT_ID, TASK_ID, ACTION_EXTERNAL_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_REJECTED)),
				false, ACTION_EXTERNAL_REVIEW, asList(ACTION_SET_STATUS_REJECTED, ACTION_SEND_EMAIL_AUTHOR, ACTION_EDIT_QUESTION), asList(ACTION_EDIT_QUESTION));
		assertResult(workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_EDIT_QUESTION, Collections.singletonMap(PARAM_EDIT_OUTCOME, VALUE_EDITED)),
				false, ACTION_EDIT_QUESTION, asList(ACTION_SET_STATUS_REVIEW, ACTION_EXTERNAL_REVIEW), asList(ACTION_EXTERNAL_REVIEW));
	}
	
	@Test
	public void testExternalReviewTimeout() {
		prepareProfileRoles(USER_ID, ROLE_EXTERNAL);
		setFireTimersImmediately(true);
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_TRANSLATION_TYPE, VALUE_EXTERNAL)), 
				false, null, asList(ACTION_SET_STATUS_REVIEW, ACTION_EXTERNAL_REVIEW), asList(ACTION_EXTERNAL_REVIEW));

		// Wait for timer to trigger
		receivedEvents.clear();
		RetriedAssert.assertWithWait(() -> assertFalse(receivedEvents.isEmpty()));
		assertThat(receivedEvents, contains(ACTION_SET_PRIORITY_HIGH, ACTION_INTERNAL_REVIEW));

		// Wait for state to change
		RetriedAssert.assertWithWait(() -> assertResult(workflowWrapper.getState(TASK_ID), false, null, emptyList(), asList(ACTION_INTERNAL_REVIEW)));
	}
	
	@Test
	public void testAutomaticTranslation() {
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_TRANSLATION_TYPE, VALUE_AUTOMATIC)), 
				false, null, asList(ACTION_SET_STATUS_AUTOMATIC_TRANSLATION, ACTION_AUTOMATIC_TRANSLATION), asList(ACTION_AUTOMATIC_TRANSLATION));
		assertResult(workflowWrapper.doAction(USER_ID_2, TENANT_ID, TASK_ID, ACTION_AUTOMATIC_TRANSLATION, Collections.singletonMap(PARAM_RESULT, VALUE_SUCCESS)),
				false, ACTION_AUTOMATIC_TRANSLATION, asList(ACTION_SET_STATUS_APPROVED, ACTION_PUBLISH), asList(ACTION_PUBLISH));
		assertResult(workflowWrapper.doAction(USER_ID_2, TENANT_ID, TASK_ID, ACTION_PUBLISH, Collections.singletonMap(PARAM_RESULT, VALUE_SUCCESS)),
				true, ACTION_PUBLISH, asList(ACTION_SET_STATUS_PUBLISHED), emptyList());
	}
	
	@Test
	public void testAutomaticTranslationFailed() {
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_TRANSLATION_TYPE, VALUE_AUTOMATIC)), 
				false, null, asList(ACTION_SET_STATUS_AUTOMATIC_TRANSLATION, ACTION_AUTOMATIC_TRANSLATION), asList(ACTION_AUTOMATIC_TRANSLATION));
		assertResult(workflowWrapper.doAction(USER_ID_2, TENANT_ID, TASK_ID, ACTION_AUTOMATIC_TRANSLATION, Collections.singletonMap(PARAM_RESULT, VALUE_FAILURE)),
				true, ACTION_AUTOMATIC_TRANSLATION, asList(ACTION_SEND_EMAIL_ADMIN), emptyList());
	}
	
}
