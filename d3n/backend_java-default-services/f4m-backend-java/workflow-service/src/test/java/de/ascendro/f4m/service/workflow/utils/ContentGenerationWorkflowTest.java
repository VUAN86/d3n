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

public class ContentGenerationWorkflowTest extends WorkflowTestBase {

	private static final String PROCESS_ID = "ContentGenerationWorkflow";

	private static final int REJECTS_NEEDED_FOR_TOTAL_REJECT = 2;
	private static final int APPROVALS_NEEDED_FOR_TOTAL_APPROVE = 3;
	
	@Override
	protected List<Resource> getResourceList() {
		return Arrays.asList(
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/ContentGenerationWorkflow.bpmn"),
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/ArchivingWorkflow.bpmn"),
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/RejectionWorkflow.bpmn"),
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/PublishingWorkflow.bpmn"),
				ResourceFactory.newClassPathResource("de/ascendro/f4m/service/workflow/process/ErrorWorkflow.bpmn"));
	}

	/**
	 * Successful community review.
	 */
	@Test
	public void testCommunityApproved() {
		prepareProfileRoles(USER_ID, ROLE_COMMUNITY);
		
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_REVIEW_TYPE, VALUE_COMMUNITY)), 
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

		// 3 approvals with different roles
		final String userNameInternal="user_internal";
		final String userNameExternal="user_external";
		final String userNameCommunity="user_community";
		
		prepareProfileRoles(userNameInternal, ROLE_INTERNAL);
		assertResult(workflowWrapper.doAction(userNameInternal, TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED)),
				false, ACTION_COMMUNITY_REVIEW, asList(ACTION_COMMUNITY_REVIEW), asList(ACTION_COMMUNITY_REVIEW));
		
		// Same user may not review twice
		try {
			workflowWrapper.doAction(userNameInternal, TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED));
			fail("Same user may not review twice");
		} catch (F4MInsufficientRightsException e) {
			// expected
		}
		
		prepareProfileRoles(userNameExternal, ROLE_EXTERNAL);
		assertResult(workflowWrapper.doAction(userNameExternal, TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED)),
				false, ACTION_COMMUNITY_REVIEW, asList(ACTION_COMMUNITY_REVIEW), asList(ACTION_COMMUNITY_REVIEW));
		
		// last positive review necessary moves forward
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
		
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_REVIEW_TYPE, VALUE_COMMUNITY)), 
				false, null, asList(ACTION_SET_STATUS_REVIEW, ACTION_COMMUNITY_REVIEW), asList(ACTION_COMMUNITY_REVIEW));
		
		// not enough negative reviews to reject
		for (int i = 0; i < REJECTS_NEEDED_FOR_TOTAL_REJECT - 1; i++) {
			prepareProfileRoles("user" + i, ROLE_COMMUNITY);
			assertResult(workflowWrapper.doAction("user" + i, TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_REJECTED)),
					false, ACTION_COMMUNITY_REVIEW, asList(ACTION_COMMUNITY_REVIEW), asList(ACTION_COMMUNITY_REVIEW));
		}
		
		// last needed negative review moves forward
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
		
		// not enough negative reviews to reject
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
	
	@Test
	public void testRejectTimeout() {
		prepareProfileRoles(USER_ID, ROLE_COMMUNITY);
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_REVIEW_TYPE, VALUE_COMMUNITY)), 
				false, null, asList(ACTION_SET_STATUS_REVIEW, ACTION_COMMUNITY_REVIEW), asList(ACTION_COMMUNITY_REVIEW));
		
		// not enough negative reviews to reject
		for (int i = 0; i < REJECTS_NEEDED_FOR_TOTAL_REJECT - 1 ; i++) {
			prepareProfileRoles("user" + i, ROLE_COMMUNITY);
			assertResult(workflowWrapper.doAction("user" + i, TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_REJECTED)),
					false, ACTION_COMMUNITY_REVIEW, asList(ACTION_COMMUNITY_REVIEW), asList(ACTION_COMMUNITY_REVIEW));
		}
		prepareProfileRoles("user3", ROLE_COMMUNITY);
		setFireTimersImmediately(true);
		assertResult(workflowWrapper.doAction("user3", TENANT_ID, TASK_ID, ACTION_COMMUNITY_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_REJECTED)),
				false, ACTION_COMMUNITY_REVIEW, asList(ACTION_SET_STATUS_REJECTED, ACTION_SEND_EMAIL_AUTHOR, ACTION_EDIT_QUESTION), asList(ACTION_EDIT_QUESTION));
		receivedEvents.clear();
		
		// Wait for timer to trigger
		RetriedAssert.assertWithWait(() -> assertFalse(receivedEvents.isEmpty()));
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
		
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_REVIEW_TYPE, VALUE_COMMUNITY)), 
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
		prepareProfileRoles(USER_ID, ROLE_COMMUNITY);
		prepareProfileRoles(USER_ID_2, ROLE_INTERNAL);
		
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_REVIEW_TYPE, VALUE_INTERNAL)), 
				false, null, asList(ACTION_SET_STATUS_REVIEW, ACTION_INTERNAL_REVIEW), asList(ACTION_INTERNAL_REVIEW));
		
		// Author may not review his own question
		try {
			workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_INTERNAL_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED));
			fail("Author may not review his own content");
		} catch (F4MInsufficientRightsException e) {
			// expected
		}
		        
		//Community member cannot review internal tasks
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
	public void testInternalReviewAdmin() {
		prepareProfileRoles(USER_ID, ROLE_COMMUNITY);
		prepareProfileRoles(USER_ID_2, ROLE_ADMIN);
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_REVIEW_TYPE, VALUE_INTERNAL)), 
				false, null, asList(ACTION_SET_STATUS_REVIEW, ACTION_INTERNAL_REVIEW), asList(ACTION_INTERNAL_REVIEW));
		
		// Author may not review his own question
		try {
			workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_INTERNAL_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_APPROVED));
			fail("Author may not review his own content");
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
		prepareProfileRoles(USER_ID, ROLE_COMMUNITY);
		prepareProfileRoles(USER_ID_2, ROLE_INTERNAL);
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_REVIEW_TYPE, VALUE_INTERNAL)), 
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
		externalReview();
	}
	
	@Test
	public void testExternalReviewInternal() {
		prepareProfileRoles(USER_ID, ROLE_INTERNAL);
		prepareProfileRoles(USER_ID_2, ROLE_ADMIN);
		externalReview();
	}
	
	@Test
	public void testExternalReviewAdmin() {
		prepareProfileRoles(USER_ID, ROLE_EXTERNAL);
		prepareProfileRoles(USER_ID_2, ROLE_ADMIN);
		externalReview();
	}
	
	private void externalReview(){
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_REVIEW_TYPE, VALUE_EXTERNAL)), 
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
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_REVIEW_TYPE, VALUE_EXTERNAL)), 
				false, null, asList(ACTION_SET_STATUS_REVIEW, ACTION_EXTERNAL_REVIEW), asList(ACTION_EXTERNAL_REVIEW));
		assertResult(workflowWrapper.doAction(USER_ID_2, TENANT_ID, TASK_ID, ACTION_EXTERNAL_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_REJECTED)),
				false, ACTION_EXTERNAL_REVIEW, asList(ACTION_SET_STATUS_REJECTED, ACTION_SEND_EMAIL_AUTHOR, ACTION_EDIT_QUESTION), asList(ACTION_EDIT_QUESTION));
		assertResult(workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_EDIT_QUESTION, Collections.singletonMap(PARAM_EDIT_OUTCOME, VALUE_EDITED)),
				false, ACTION_EDIT_QUESTION, asList(ACTION_SET_STATUS_REVIEW, ACTION_EXTERNAL_REVIEW), asList(ACTION_EXTERNAL_REVIEW));
	}
	
	@Test
	public void testExternalReviewRejected_Archived() {
		prepareProfileRoles(USER_ID, ROLE_EXTERNAL);
		prepareProfileRoles(USER_ID_2, ROLE_EXTERNAL);
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_REVIEW_TYPE, VALUE_EXTERNAL)), 
				false, null, asList(ACTION_SET_STATUS_REVIEW, ACTION_EXTERNAL_REVIEW), asList(ACTION_EXTERNAL_REVIEW));
		assertResult(workflowWrapper.doAction(USER_ID_2, TENANT_ID, TASK_ID, ACTION_EXTERNAL_REVIEW, Collections.singletonMap(PARAM_REVIEW_OUTCOME, VALUE_REJECTED)),
				false, ACTION_EXTERNAL_REVIEW, asList(ACTION_SET_STATUS_REJECTED, ACTION_SEND_EMAIL_AUTHOR, ACTION_EDIT_QUESTION), asList(ACTION_EDIT_QUESTION));
		assertResult(workflowWrapper.doAction(USER_ID, TENANT_ID, TASK_ID, ACTION_EDIT_QUESTION, Collections.singletonMap(PARAM_EDIT_OUTCOME, VALUE_ARCHIVED)),
				false, ACTION_EDIT_QUESTION, asList(ACTION_ARCHIVE), asList(ACTION_ARCHIVE));
	}
	
	@Test
	public void testExternalReviewTimeout() {
		prepareProfileRoles(USER_ID, ROLE_EXTERNAL);
		setFireTimersImmediately(true);
		assertResult(workflowWrapper.startProcess(USER_ID, TENANT_ID, PROCESS_ID, TASK_ID, null, Collections.singletonMap(PARAM_REVIEW_TYPE, VALUE_EXTERNAL)), 
				false, null, asList(ACTION_SET_STATUS_REVIEW, ACTION_EXTERNAL_REVIEW), asList(ACTION_EXTERNAL_REVIEW));

		// Wait for timer to trigger
		receivedEvents.clear();
		RetriedAssert.assertWithWait(() -> assertFalse(receivedEvents.isEmpty()));
		assertThat(receivedEvents, contains(ACTION_SET_PRIORITY_HIGH, ACTION_INTERNAL_REVIEW));

		// Wait for state to change
		RetriedAssert.assertWithWait(() -> assertResult(workflowWrapper.getState(TASK_ID), false, null, emptyList(), asList(ACTION_INTERNAL_REVIEW)));
	}
	
}
