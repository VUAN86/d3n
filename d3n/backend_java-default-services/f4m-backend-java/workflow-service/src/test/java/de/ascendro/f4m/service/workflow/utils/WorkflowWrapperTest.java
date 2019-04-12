package de.ascendro.f4m.service.workflow.utils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.io.Resource;
import org.kie.internal.io.ResourceFactory;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.gson.JsonElement;

import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.exception.client.F4MEntryAlreadyExistsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.integration.RetriedAssert;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.TestJsonMessageUtil;
import de.ascendro.f4m.service.workflow.config.WorkflowConfig;

public class WorkflowWrapperTest {

	private static final String USER_ID = "userId1";
	private static final String TENANT_ID = "tenantId";

	private static final String PROCESS_SIMPLE = "SimpleProcess";
	private static final String PROCESS_SPLIT_FLOW = "SplitFlowProcess";
	private static final String PROCESS_MULTIPLE_INSTANCES = "MultipleInstancesProcess";
	private static final String PROCESS_TIMER = "TimerProcess";
	
	private static final String TASK_ID = "task1";
	private static final String ANOTHER_TASK_ID = "task2";
	
	private WorkflowWrapper workflowWrapper;
	
	private JsonMessageUtil jsonMessageUtil = new TestJsonMessageUtil();
	
	@Mock
	private EventServiceClient eventServiceClient;
	
	@Mock
	private CommonProfileAerospikeDao profileDao;
	
	private final Map<String, List<JsonElement>> receivedEvents = new HashMap<>();
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				String topic = invocation.getArgument(0);
				List<JsonElement> publishedEvents = receivedEvents.get(topic); // 1st argument - topic
				if (publishedEvents == null) {
					publishedEvents = new ArrayList<JsonElement>();
					receivedEvents.put(topic, publishedEvents);
				}
				publishedEvents.add(invocation.getArgument(1)); // 2nd argument - published content
				return null;
			}
		}).when(eventServiceClient).publish(anyString(), any(JsonElement.class));
		
		WorkflowConfig config = new WorkflowConfig();
		workflowWrapper = new WorkflowWrapper(
				config, new WorkflowResourceProvider() {
					@Override
					public List<Resource> getResources() {
						return Arrays.asList(
								ResourceFactory.newClassPathResource("simpleTest.bpmn", getClass()),
								ResourceFactory.newClassPathResource("splitFlowTest.bpmn", getClass()),
								ResourceFactory.newClassPathResource("multipleInstancesTest.bpmn", getClass()),
								ResourceFactory.newClassPathResource("timerTest.bpmn", getClass()));
					}
				},
				profileDao,
				jsonMessageUtil,
				eventServiceClient);
	}
	
	@After
	public void tearDown() {
		if (workflowWrapper != null) {
			workflowWrapper.close();
		}
	}
	
	
	@Test
	public void testStartAndFinishProcessThrowsAppropriateExceptions() {
		// Start two tasks
		assertResult(startTask(TASK_ID, PROCESS_SIMPLE, true), false, null, asList("review"), asList("review"));
		assertResult(startTask(ANOTHER_TASK_ID, PROCESS_SIMPLE, true), false, null, asList("review"), asList("review"));
		
		// Finish two tasks by executing actions (there is just one action in the process)
		assertResult(doAction(TASK_ID, "review", true), true, "review", emptyList(), emptyList());
		assertResult(doAction(ANOTHER_TASK_ID, "review", true), true, "review", emptyList(), emptyList());
	}

	@Test
	public void testSplitFlow() {
		assertResult(startTask(TASK_ID, PROCESS_SPLIT_FLOW, false), false, null, asList("drink beer", "review"), asList("drink beer", "review"));
		assertResult(startTask(ANOTHER_TASK_ID, PROCESS_SPLIT_FLOW, false), false, null, asList("drink beer", "review"), asList("drink beer", "review"));
		
		// Do review on first
		assertResult(doAction(TASK_ID, "review", false), false, "review", emptyList(), asList("drink beer"));
		assertResult(workflowWrapper.getState(TASK_ID), false, null, emptyList(), asList("drink beer"));

		// Do drink beer on second
		assertResult(doAction(ANOTHER_TASK_ID, "drink beer", false), false, "drink beer", asList("drink whiskey", "go to toilet"), asList("drink whiskey", "go to toilet", "review"));
		assertResult(workflowWrapper.getState(ANOTHER_TASK_ID), false, null, emptyList(), asList("drink whiskey", "go to toilet", "review"));
		
		// Do drink beer on first
		assertResult(doAction(TASK_ID, "drink beer", false), false, "drink beer", asList("drink whiskey", "go to toilet"), asList("drink whiskey", "go to toilet"));
		
		// Do review on second
		assertResult(doAction(ANOTHER_TASK_ID, "review", false), false, "review", emptyList(), asList("drink whiskey", "go to toilet"));
		
		// Do drink whiskey on first
		assertResult(doAction(TASK_ID, "drink whiskey", false), false, "drink whiskey", emptyList(), asList("go to toilet"));
		
		// Do go to toilet on second
		assertResult(doAction(ANOTHER_TASK_ID, "go to toilet", false), false, "go to toilet", emptyList(), asList("drink whiskey"));
		
		// Do go to toilet on first
		assertResult(doAction(TASK_ID, "go to toilet", false), true, "go to toilet", emptyList(), emptyList());
		assertResult(workflowWrapper.getState(TASK_ID), true, null, emptyList(), emptyList());
		
		// Do drink whiskey on second
		assertResult(doAction(ANOTHER_TASK_ID, "drink whiskey", false), true, "drink whiskey", emptyList(), emptyList());
	}
	
	@Test
	public void testMultipleInstances() {
		assertResult(startTask(TASK_ID, PROCESS_MULTIPLE_INSTANCES, false), false, null, asList("review", "review", "review"), asList("review"));
		assertResult(doAction(TASK_ID, "review", false), false, "review", emptyList(), asList("review"));
		assertResult(doAction(TASK_ID, "review", false), false, "review", emptyList(), asList("review"));
		assertResult(doAction(TASK_ID, "review", false), true, "review", emptyList(), emptyList());
	}

	@Test
	public void testTimer() throws Exception {
		assertResult(startTask(TASK_ID, PROCESS_TIMER, false), false, null, asList("review"), asList("review"));
		RetriedAssert.assertWithWait(() -> assertEquals(2, receivedEvents.get("workflow/*").size()));
		assertEquals("{\"taskType\":\"" + PROCESS_TIMER + "\",\"taskId\":\"" + TASK_ID + "\",\"triggeredAction\":\"review\"}", receivedEvents.get("workflow/*").get(0).toString());
		assertEquals("{\"taskType\":\"" + PROCESS_TIMER + "\",\"taskId\":\"" + TASK_ID + "\",\"triggeredAction\":\"archive\"}", receivedEvents.get("workflow/*").get(1).toString());
	}
	
	private ActionResult doAction(String taskId, String actionType, boolean testExecutingOnceMore) {
		ActionResult result = workflowWrapper.doAction(USER_ID, TENANT_ID, taskId, actionType, null);
		if (testExecutingOnceMore) {
			try {
				workflowWrapper.doAction(USER_ID, TENANT_ID, taskId, actionType, null);
				fail("Process should be finished by now");
			} catch (F4MEntryNotFoundException e) {
				// Expected to fail
			}
		}
		return result;
	}
	
	private ActionResult startTask(String taskId, String processId, boolean testStartingAnother) {
		ActionResult result = workflowWrapper.startProcess(USER_ID, TENANT_ID, processId, taskId, null, null);
		if (testStartingAnother) {
			try {
				workflowWrapper.startProcess(USER_ID, TENANT_ID, processId, taskId, null, null);
				fail("May not start a process with same task ID");
			} catch (F4MEntryAlreadyExistsException e) {
				// Expected to fail
			}
		}
		return result;
	}
	
	private void assertResult(ActionResult result, boolean processFinished, String previousState, 
			List<String> newStatesTriggered, List<String> availableStates) {
		assertEquals(processFinished, result.isProcessFinished());
		assertEquals(previousState, result.getPreviousState());
		assertThat(result.getNewStatesTriggered(), containsInAnyOrder(newStatesTriggered.toArray()));
		assertThat(result.getAvailableStates(), containsInAnyOrder(availableStates.toArray()));
	}

}
