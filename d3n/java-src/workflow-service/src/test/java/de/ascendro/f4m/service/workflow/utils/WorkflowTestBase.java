package de.ascendro.f4m.service.workflow.utils;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.drools.core.time.InternalSchedulerService;
import org.drools.core.time.Job;
import org.drools.core.time.JobContext;
import org.drools.core.time.JobHandle;
import org.drools.core.time.SelfRemovalJobContext;
import org.drools.core.time.TimerService;
import org.drools.core.time.Trigger;
import org.drools.core.time.impl.TimerJobInstance;
import org.jbpm.process.core.timer.SchedulerServiceInterceptor;
import org.jbpm.process.core.timer.impl.DelegateSchedulerServiceInterceptor;
import org.jbpm.process.core.timer.impl.ThreadPoolSchedulerService;
import org.jbpm.process.instance.timer.TimerManager.ProcessJobContext;
import org.jbpm.process.instance.timer.TimerManager.StartProcessJobContext;
import org.junit.After;
import org.junit.Before;
import org.kie.api.io.Resource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.util.TestJsonMessageUtil;
import de.ascendro.f4m.service.workflow.config.WorkflowConfig;

public abstract class WorkflowTestBase {

	protected static final String ACTION_PUBLISH = "Publish";
	protected static final String ACTION_UNPUBLISH = "Unpublish";
	protected static final String ACTION_INTERNAL_REVIEW = "InternalReview";
	protected static final String ACTION_EXTERNAL_REVIEW = "ExternalReview";
	protected static final String ACTION_COMMUNITY_REVIEW = "CommunityReview";
	protected static final String ACTION_AUTOMATIC_TRANSLATION = "AutomaticTranslation";
	protected static final String ACTION_SET_PRIORITY_HIGH = "SetPriority(High)";
	protected static final String ACTION_SET_STATUS_REVIEW = "SetStatus(Review)";
	protected static final String ACTION_SET_STATUS_AUTOMATIC_TRANSLATION = "SetStatus(AutomaticTranslation)";
	protected static final String ACTION_SET_STATUS_APPROVED = "SetStatus(Approved)";
	protected static final String ACTION_SET_STATUS_REJECTED = "SetStatus(Rejected)";
	protected static final String ACTION_SET_STATUS_PUBLISHED = "SetStatus(Published)";
	protected static final String ACTION_SET_STATUS_UNPUBLISHED = "SetStatus(Unpublished)";
	protected static final String ACTION_SET_STATUS_ARCHIVED = "SetStatus(Archived)";
	protected static final String ACTION_SEND_EMAIL_AUTHOR = "SendEmail(Author)";
	protected static final String ACTION_SEND_EMAIL_ADMIN = "SendEmail(Admin)";
	protected static final String ACTION_EDIT_QUESTION = "EditQuestion";
	protected static final String ACTION_ARCHIVE = "Archive";

	protected static final String PARAM_REVIEW_OUTCOME = "reviewOutcome";
	protected static final String VALUE_APPROVED = "Approved";
	protected static final String VALUE_REJECTED = "Rejected";
	
	protected static final String PARAM_EDIT_OUTCOME = "editOutcome";
	protected static final String VALUE_EDITED = "Edited";
	protected static final String VALUE_ARCHIVED = "Archived";
	
	protected static final String PARAM_REVIEW_TYPE = "reviewType";
	protected static final String VALUE_COMMUNITY = "COMMUNITY";
	protected static final String VALUE_INTERNAL = "INTERNAL";
	protected static final String VALUE_EXTERNAL = "EXTERNAL";

	protected static final String PARAM_TRANSLATION_TYPE = "translationType";
	protected static final String VALUE_AUTOMATIC = "AUTOMATIC";
	
	protected static final String PARAM_RESULT = "result";
	protected static final String VALUE_SUCCESS = "Success";
	protected static final String VALUE_FAILURE = "Failure";
	
	protected static final String PARAM_ARCHIVE = "archive";
	protected static final boolean VALUE_TRUE = true;
	protected static final boolean VALUE_FALSE = false;

	protected static final String PARAM_ITEM_STATUS = "itemStatus";
	protected static final String VALUE_PUBLISHED = "Published";
	protected static final String VALUE_UNPUBLISHED = "Unpublished";
	
	protected static final String ROLE_COMMUNITY = "COMMUNITY";
	protected static final String ROLE_INTERNAL = "INTERNAL";
	protected static final String ROLE_EXTERNAL = "EXTERNAL";
	protected static final String ROLE_ADMIN = "ADMIN";
	
	protected static final String USER_ID = "user";
	protected static final String USER_ID_2 = "anotheruser";

	protected static final String TENANT_ID = "tenantId";
	
	protected static final String TASK_ID = "task1";
	
	protected WorkflowWrapper workflowWrapper;
	
	@Mock
	private CommonProfileAerospikeDao profileDao;
	
	private JsonMessageUtil jsonMessageUtil = new TestJsonMessageUtil();
	
	@Mock
	private EventServiceClient eventServiceClient;

	private AtomicBoolean fireTimersImmediately = new AtomicBoolean(false);

	protected final List<String> receivedEvents = new ArrayList<>();
	
	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				JsonObject evt = (JsonObject) invocation.getArgument(1);
				receivedEvents.add(evt.get("triggeredAction").getAsString()); // 2nd argument - published content
				return null;
			}
		}).when(eventServiceClient).publish(anyString(), any(JsonElement.class));
		
		WorkflowConfig config = new WorkflowConfig();
		workflowWrapper = new WorkflowWrapper(config, new WorkflowResourceProvider() {
			@Override
			public List<Resource> getResources() {
				return getResourceList();
			}
		}, profileDao, jsonMessageUtil, eventServiceClient) {
			@Override
			protected ThreadPoolSchedulerService prepareScheduler() {
				// Prepare customized scheduler service allowing to change scheduled interval and minimizing shutdown time
				return new ThreadPoolSchedulerService(3) {

					private AtomicLong idCounter = new AtomicLong();
					private ScheduledThreadPoolExecutor scheduler;
					private TimerService globalTimerService;
					private SchedulerServiceInterceptor interceptor = new DelegateSchedulerServiceInterceptor(this);

					private ConcurrentHashMap<String, JobHandle> activeTimer = new ConcurrentHashMap<String, JobHandle>();

					@Override
					public void initScheduler(TimerService globalTimerService) {
						this.globalTimerService = globalTimerService;
						this.scheduler = new ScheduledThreadPoolExecutor(1);
					}

					@Override
					public void shutdown() {
						this.scheduler.shutdownNow();
					}

					@Override
					public JobHandle scheduleJob(Job job, JobContext ctx, Trigger trigger) {
						Date date = trigger.hasNextFireTime();
						if (date != null) {
							String jobname = null;
							if (ctx instanceof ProcessJobContext) {
								ProcessJobContext processCtx = (ProcessJobContext) ctx;
								jobname = processCtx.getSessionId() + "-" + processCtx.getProcessInstanceId() + "-"
										+ processCtx.getTimer().getId();
								if (processCtx instanceof StartProcessJobContext) {
									jobname = "StartProcess-" + ((StartProcessJobContext) processCtx).getProcessId()
											+ "-" + processCtx.getTimer().getId();
								}
								if (activeTimer.containsKey(jobname)) {
									return activeTimer.get(jobname);
								}

							}
							GlobalJDKJobHandle jobHandle = new GlobalJDKJobHandle(idCounter.getAndIncrement());

							TimerJobInstance jobInstance = globalTimerService.getTimerJobFactoryManager()
									.createTimerJobInstance(job, ctx, trigger, jobHandle,
											(InternalSchedulerService) globalTimerService);
							jobHandle.setTimerJobInstance((TimerJobInstance) jobInstance);
							interceptor.internalSchedule((TimerJobInstance) jobInstance);
							if (jobname != null) {
								activeTimer.put(jobname, jobHandle);
							}
							return jobHandle;
						} else {
							return null;
						}

					}

					@Override
					public boolean removeJob(JobHandle jobHandle) {
						if (jobHandle == null) {
							return false;
						}
						jobHandle.setCancel(true);
						JobContext jobContext = ((GlobalJDKJobHandle) jobHandle).getTimerJobInstance().getJobContext();
						try {
							ProcessJobContext processCtx = null;
							if (jobContext instanceof SelfRemovalJobContext) {
								processCtx = (ProcessJobContext) ((SelfRemovalJobContext) jobContext).getJobContext();
							} else {
								processCtx = (ProcessJobContext) jobContext;
							}

							String jobname = processCtx.getSessionId() + "-" + processCtx.getProcessInstanceId() + "-"
									+ processCtx.getTimer().getId();
							if (processCtx instanceof StartProcessJobContext) {
								jobname = "StartProcess-" + ((StartProcessJobContext) processCtx).getProcessId() + "-"
										+ processCtx.getTimer().getId();
							}
							activeTimer.remove(jobname);
							globalTimerService.getTimerJobFactoryManager()
									.removeTimerJobInstance(((GlobalJDKJobHandle) jobHandle).getTimerJobInstance());
						} catch (ClassCastException e) {
							// do nothing in case ProcessJobContext was not given
						}
						boolean removed = this.scheduler
								.remove((Runnable) ((GlobalJDKJobHandle) jobHandle).getFuture());
						return removed;
					}

					@Override
					public void internalSchedule(TimerJobInstance timerJobInstance) {
						if (scheduler.isShutdown()) {
							return;
						}
						Date date = timerJobInstance.getTrigger().hasNextFireTime();
						@SuppressWarnings("unchecked")
						Callable<Void> item = (Callable<Void>) timerJobInstance;

						GlobalJDKJobHandle jobHandle = (GlobalJDKJobHandle) timerJobInstance.getJobHandle();
						long then = date.getTime();
						long now = System.currentTimeMillis();
						ScheduledFuture<Void> future = null;
						if (then >= now && ! fireTimersImmediately.get()) {
							future = scheduler.schedule(item, then - now, TimeUnit.MILLISECONDS);
						} else {
							future = scheduler.schedule(item, 1, TimeUnit.SECONDS);
						}

						jobHandle.setFuture(future);
						globalTimerService.getTimerJobFactoryManager().addTimerJobInstance(timerJobInstance);
					}
				};
			}
		};
	}
	
	@After
	public void tearDown() {
		if (workflowWrapper != null) {
			workflowWrapper.close();
		}
	}
	
	protected abstract List<Resource> getResourceList();

	protected void assertResult(ActionResult result, boolean processFinished, String previousState, 
			List<String> newStatesTriggered, List<String> availableStates) {
		assertEquals(processFinished, result.isProcessFinished());
		assertEquals(previousState, result.getPreviousState());
		assertThat(result.getNewStatesTriggered(), containsInAnyOrder(newStatesTriggered.toArray()));
		assertThat(result.getAvailableStates(), containsInAnyOrder(availableStates.toArray()));
	}

	protected void prepareProfileRoles(String userId, String... roles) {
		Profile profile = new Profile();
		JsonArray roleArray = new JsonArray();
		for (String role : roles) {
			roleArray.add(Profile.TENANT_ROLE_PREFIX + TENANT_ID + Profile.TENANT_ROLE_SEPARATOR + role);
		}
		profile.setProperty(Profile.ROLES_PROPERTY, roleArray);
		when(profileDao.getProfile(userId)).thenReturn(profile);
	}

	protected void setFireTimersImmediately(boolean fireTimersImmediately) {
		this.fireTimersImmediately.set(fireTimersImmediately);
	}

}
