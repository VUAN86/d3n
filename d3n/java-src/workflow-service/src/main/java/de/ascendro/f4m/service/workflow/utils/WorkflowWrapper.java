package de.ascendro.f4m.service.workflow.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.SystemException;

import org.apache.commons.lang3.StringUtils;
import org.jbpm.persistence.processinstance.ProcessInstanceInfo;
import org.jbpm.process.audit.VariableInstanceLog;
import org.jbpm.process.core.Context;
import org.jbpm.process.core.ContextContainer;
import org.jbpm.process.core.timer.impl.ThreadPoolSchedulerService;
import org.jbpm.process.instance.impl.demo.DoNothingWorkItemHandler;
import org.jbpm.runtime.manager.impl.DefaultRegisterableItemsFactory;
import org.jbpm.runtime.manager.impl.RuntimeEnvironmentBuilder;
import org.jbpm.services.task.commands.TaskCommand;
import org.jbpm.services.task.commands.TaskContext;
import org.jbpm.services.task.impl.model.TaskImpl;
import org.jbpm.services.task.utils.ClassUtil;
import org.jbpm.workflow.core.Node;
import org.jbpm.workflow.core.node.DataAssociation;
import org.jbpm.workflow.core.node.SubProcessNode;
import org.jbpm.workflow.instance.WorkflowRuntimeException;
import org.jbpm.workflow.instance.impl.NodeInstanceFactoryRegistry;
import org.jbpm.workflow.instance.impl.factory.CreateNewNodeFactory;
import org.jbpm.workflow.instance.node.SubProcessNodeInstance;
import org.kie.api.KieBase;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.io.Resource;
import org.kie.api.runtime.EnvironmentName;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.api.task.UserGroupCallback;
import org.kie.api.task.model.Group;
import org.kie.api.task.model.OrganizationalEntity;
import org.kie.api.task.model.Status;
import org.kie.api.task.model.Task;
import org.kie.api.task.model.User;
import org.kie.internal.KieInternalServices;
import org.kie.internal.process.CorrelationAwareProcessRuntime;
import org.kie.internal.process.CorrelationKey;
import org.kie.internal.process.CorrelationKeyFactory;
import org.kie.internal.runtime.manager.RuntimeEnvironment;
import org.kie.internal.runtime.manager.context.CorrelationKeyContext;
import org.kie.internal.task.api.InternalTaskService;
import org.kie.internal.task.api.TaskPersistenceContext;
import org.kie.internal.utils.KieHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mycila.guice.ext.closeable.InjectorCloseListener;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import bitronix.tm.resource.jdbc.lrc.LrcXADataSource;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.service.exception.F4MException;
import de.ascendro.f4m.service.exception.auth.F4MInsufficientRightsException;
import de.ascendro.f4m.service.exception.client.F4MEntryAlreadyExistsException;
import de.ascendro.f4m.service.exception.client.F4MEntryNotFoundException;
import de.ascendro.f4m.service.exception.server.F4MFatalErrorException;
import de.ascendro.f4m.service.json.JsonMessageUtil;
import de.ascendro.f4m.service.profile.model.Profile;
import de.ascendro.f4m.service.util.EventServiceClient;
import de.ascendro.f4m.service.workflow.config.WorkflowConfig;

public class WorkflowWrapper implements InjectorCloseListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowWrapper.class);
	
	public static final String PARAM_DESCRIPTION = "description";
	public static final String PARAM_USER_STARTED_ID = "userStartedId";
	public static final String PARAM_USER_STARTED_ROLES = "userStartedRoles";
	public static final String PARAM_USER_ID = "userId";
	public static final String PARAM_USER_ROLES = "userRoles";
	public static final String PARAM_TASK_ID = "taskId";

	private static final String JBPM_DS_NAME = "jdbc/jbpm-ds";
	private static final String GROUP_ANY = "anyone";
	private static final long MAX_CACHED_USERS = 100;
	private static final long MAX_CACHED_TIME_SECONDS = 30;

	static final String SEPARATOR = ":";
	
	private final RuntimeManager manager;
	private final CorrelationKeyFactory correlationKeyFactory;
	private final BitronixTransactionManager transactionManager;
	private final PoolingDataSource dataSource;
	private final EntityManagerFactory entityManagerFactory;
	private final ThreadPoolSchedulerService schedulerService;
	private final LoadingCache<String, List<String>> userRoleCache;
	
	@Inject
	public WorkflowWrapper(WorkflowConfig config, WorkflowResourceProvider resourceProvider, CommonProfileAerospikeDao profileDao,
			JsonMessageUtil jsonUtil, EventServiceClient eventServiceClient) {
		// Build user role cache
		userRoleCache = CacheBuilder.newBuilder()
				.maximumSize(MAX_CACHED_USERS)
				.expireAfterWrite(MAX_CACHED_TIME_SECONDS, TimeUnit.SECONDS)
				.build(new CacheLoader<String, List<String>>() {
					@Override
					public List<String> load(String userAndTenantId) throws Exception {
						String userId = StringUtils.substringBefore(userAndTenantId, SEPARATOR);
						String tenantId = StringUtils.substringAfter(userAndTenantId, SEPARATOR);
						Profile profile = profileDao.getProfile(userId);
						if (profile == null) {
							return Collections.singletonList(GROUP_ANY);
						} else {
							List<String> result = new ArrayList<>(Arrays.asList(profile.getRoles(tenantId)));
							result.add(GROUP_ANY);
							return result;
						}
					}
				});

		// Build knowledge base
		KieHelper helper = new KieHelper();
		for (Resource resource : resourceProvider.getResources()) {
			helper.addResource(resource);
		}
		KieBase knowledgeBase = helper.build();
		
		// Build correlation key factory
		correlationKeyFactory = KieInternalServices.Factory.get().newCorrelationKeyFactory();
		
		// Build data source
		String dsClassName = config.getProperty(WorkflowConfig.DS_CLASS_NAME);
        dataSource = new PoolingDataSource();
        dataSource.setUniqueName(JBPM_DS_NAME);
        dataSource.setClassName(dsClassName);
        dataSource.setMaxPoolSize(config.getPropertyAsInteger(WorkflowConfig.DS_MAX_POOL_SIZE));
        dataSource.setAllowLocalTransactions(true);
        dataSource.getDriverProperties().put("user", config.getProperty(WorkflowConfig.DS_USER));
        dataSource.getDriverProperties().put("password", config.getProperty(WorkflowConfig.DS_PASSWORD));
        dataSource.getDriverProperties().put("url", config.getProperty(WorkflowConfig.DS_URL));
        if (LrcXADataSource.class.getName().equals(dsClassName)) {
        	dataSource.getDriverProperties().put("driverClassName", config.getProperty(WorkflowConfig.DS_DRIVER_CLASS_NAME));
        }
        dataSource.init();

        // Build user-group callback
		UserGroupCallback userGroupCallback = new UserGroupCallback() {
			@Override
			public List<String> getGroupsForUser(String userAndTenantId, List<String> groupIds, List<String> allExistingGroupIds) {
				return userRoleCache.getUnchecked(userAndTenantId);
			}
			
			@Override
			public boolean existsUser(String userId) {
				return true;
			}
			
			@Override
			public boolean existsGroup(String groupId) {
				return true;
			}
		};
		
		// Build runtime
		Configuration conf = TransactionManagerServices.getConfiguration();
		try {
			conf.setServerId(InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) {
			LOGGER.warn("Could not get server IP", e);
			conf.setServerId(UUID.randomUUID().toString());
		}
		conf.setWarnAboutZeroResourceTransaction(false);
		transactionManager = TransactionManagerServices.getTransactionManager();
		Map<String, String> persistenceProperties = new HashMap<>();
		persistenceProperties.put("hibernate.hbm2ddl.auto", config.getProperty(WorkflowConfig.HIBERNATE_GENERATE_SCHEMA));
		persistenceProperties.put("hibernate.dialect", config.getProperty(WorkflowConfig.HIBERNATE_DIALECT));
		entityManagerFactory = Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa", persistenceProperties);
		schedulerService = prepareScheduler();
		RuntimeEnvironment environment = RuntimeEnvironmentBuilder.getDefault()
				.entityManagerFactory(entityManagerFactory)
				.schedulerService(schedulerService)
				.userGroupCallback(userGroupCallback)
				.knowledgeBase(knowledgeBase)
				.persistence(true)
				.addEnvironmentEntry(EnvironmentName.TRANSACTION_MANAGER, transactionManager)
				.registerableItemsFactory(new DefaultRegisterableItemsFactory() {
					@Override
					public Map<String, WorkItemHandler> getWorkItemHandlers(RuntimeEngine runtime) {
						Map<String, WorkItemHandler> handlers = super.getWorkItemHandlers(runtime);
						handlers.put("Service Task", new DoNothingWorkItemHandler() {
							@Override
							public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
								manager.completeWorkItem(workItem.getId(), Collections.emptyMap());
							}
						});
						return handlers;
					}
					@Override
					public List<ProcessEventListener> getProcessEventListeners(RuntimeEngine runtime) {
						List<ProcessEventListener> listeners = super.getProcessEventListeners(runtime);
						listeners.add(new EventSendingProcessEventListener(transactionManager, jsonUtil, eventServiceClient));
						return listeners;
					}
				})
				.get();
		NodeInstanceFactoryRegistry.getInstance(environment.getEnvironment()).register(SubProcessNode.class,
				new CreateNewNodeFactory(ExtendedSubProcessNodeInstance.class));
		manager = RuntimeManagerFactory.Factory.get().newPerRequestRuntimeManager(environment);
		
	}

	protected ThreadPoolSchedulerService prepareScheduler() {
		return new ThreadPoolSchedulerService(3);
	}
	
	/**
	 * Start the JBPM process.
	 * @return List of human tasks that process has stopped on. Can be empty, 
	 */
	public ActionResult startProcess(String userId, String tenantId, String taskType, String taskId, String description, Map<String, Object> parameters) {
		String userAndTenantId = getUserAndTenantId(userId, tenantId);
		Map<String, Object> params = parameters == null ? new HashMap<>(4) : new HashMap<>(parameters);
		params.put(PARAM_USER_STARTED_ID, userAndTenantId);
		params.put(PARAM_DESCRIPTION, description);
		params.put(PARAM_USER_STARTED_ROLES, userRoleCache.getUnchecked(userAndTenantId));
		params.put(PARAM_TASK_ID, taskId);
		RuntimeEngine runtime = manager.getRuntimeEngine(CorrelationKeyContext.get());
		try {
			UserTaskGatheringProcessEventListener listener = new UserTaskGatheringProcessEventListener();
			executeWithinTransaction(() -> {
				KieSession session = runtime.getKieSession();
				session.addEventListener(listener);
				CorrelationKey key = correlationKeyFactory.newCorrelationKey(taskId);
				try {
					((CorrelationAwareProcessRuntime) session).startProcess(taskType, key, params);
				} catch (RuntimeException e) {
					// Unfortunately jBPM does not have typed exceptions, so have to use this "hack"
					if (e.getMessage() != null && e.getMessage().contains("already exists")) {
						throw new F4MEntryAlreadyExistsException("Process with id {" + taskId + "} already started");
					} else if (e.getCause() instanceof F4MException) {
						throw (F4MException) e.getCause();
					} else {
						throw e;
					}
				}
				return null;
			});
			return new ActionResult(listener.getTasksTriggered(), new HashSet<String>(listener.getUserTasksTriggered()), 
					listener.isProcessFinished());
		} finally {
			try {
				manager.disposeRuntimeEngine(runtime);
			} catch (Exception e) {
				LOGGER.error("Could not dispose the runtime", e);
			}
		}
	}

	/**
	 * Perform user task
	 */
	public ActionResult doAction(String userId, String tenantId, String taskId, String actionType, Map<String, Object> parameters) {
		String userAndTenantId = getUserAndTenantId(userId, tenantId);
		Map<String, Object> params = parameters == null ? new HashMap<>(3) : new HashMap<>(parameters);
		final List<String> roles = userRoleCache.getUnchecked(userAndTenantId);
		params.put(PARAM_USER_ID, userAndTenantId);
		params.put(PARAM_USER_ROLES, roles);
		params.put(PARAM_TASK_ID, taskId);
		CorrelationKey key = correlationKeyFactory.newCorrelationKey(taskId);
		RuntimeEngine runtime = manager.getRuntimeEngine(CorrelationKeyContext.get(key));
		UserTaskGatheringProcessEventListener listener = new UserTaskGatheringProcessEventListener();
		try {
			String executedTask = executeWithinTransaction(() -> {
				// Get session
				KieSession session;
				try {
					session = runtime.getKieSession();
				} catch (RuntimeException e) { // Current jBPM implementation throws non-intuitive NullPointerException
					throw new F4MEntryNotFoundException("There is no process started for task {" + taskId + "}", e);
				}
				session.addEventListener(listener);

				// Get the process instance
				InternalTaskService taskService = (InternalTaskService) runtime.getTaskService();
				ProcessInstance processInstance = ((CorrelationAwareProcessRuntime) session).getProcessInstance(key);
				if (processInstance == null) {
					throw new F4MEntryNotFoundException("There is no process started for task {" + taskId + "}");
				}

				// Execute first task that can be done by the user
				TaskInfo firstAvailableTask = getFirstTasksThatCanBeExecutedByUser(userId, roles, taskId, actionType, taskService);
				if (firstAvailableTask == null) {
					throw new F4MInsufficientRightsException("Process {" + taskId + "} does not have any human tasks available for user {" 
								+ userAndTenantId + "} with roles {" + roles + "} for taskType {" + actionType + "}");
				}
				try {
					taskService.start(firstAvailableTask.getId(), userAndTenantId);
					taskService.complete(firstAvailableTask.getId(), userAndTenantId, params);
				} catch (WorkflowRuntimeException e) {
					if (e.getCause() instanceof F4MException) {
						throw (F4MException) e.getCause();
					} else {
						throw e;
					}
				}
				
				// Return executed task and available tasks after execution
				return firstAvailableTask.getName();
			});

			// Return result
			return new ActionResult(executedTask, listener.getTasksTriggered(), getState(taskId).getAvailableStates(), listener.isProcessFinished());
		} finally {
			try {
				manager.disposeRuntimeEngine(runtime);
			} catch (Exception e) {
				LOGGER.error("Could not dispose the runtime", e);
			}
		}
	}

	/**
	 * Get the current state of the process.
	 */
	public ActionResult getState(String taskId) {
		CorrelationKey key = correlationKeyFactory.newCorrelationKey(taskId);
		RuntimeEngine runtime = manager.getRuntimeEngine(CorrelationKeyContext.get(key));
		try {
			// Result will be null, if process is finished / not started
			Set<String> result = executeWithinTransaction(() -> {
				// Return executed task and available tasks after execution
				return getAvailableTasks(taskId, (InternalTaskService) runtime.getTaskService());
			});
			
			// Return result
			return new ActionResult(result == null ? Collections.emptySet() : result, result == null || result.isEmpty());
		} finally {
			try {
				manager.disposeRuntimeEngine(runtime);
			} catch (Exception e) {
				LOGGER.error("Could not dispose the runtime", e);
			}
		}
	}

	/**
	 * Abort the process.
	 */
	public void abort(String taskId) {
		CorrelationKey key = correlationKeyFactory.newCorrelationKey(taskId);
		RuntimeEngine runtime = manager.getRuntimeEngine(CorrelationKeyContext.get(key));
		try {
			// Result will be null, if process is finished / not started
			executeWithinTransaction(() -> {
				// Get session
				KieSession session;
				try {
					session = runtime.getKieSession();
				} catch (RuntimeException e) { // Current jBPM implementation throws non-intuitive NullPointerException
					throw new F4MEntryNotFoundException("There is no process started for task {" + taskId + "}", e);
				}

				// Get the process instance
				ProcessInstance processInstance = ((CorrelationAwareProcessRuntime) session).getProcessInstance(key);
				if (processInstance == null) {
					throw new F4MEntryNotFoundException("There is no process started for task {" + taskId + "}");
				}

				// Abort process
				session.abortProcessInstance(processInstance.getId());
				return null;
			});
		} finally {
			try {
				manager.disposeRuntimeEngine(runtime);
			} catch (Exception e) {
				LOGGER.error("Could not dispose the runtime", e);
			}
		}
	}

	/**
	 * Execute statements within transaction.
	 */
	private <T> T executeWithinTransaction(Callable<T> statements) {
		// Execute statements within transaction
		try {
			transactionManager.begin();
			T result = statements.call();
			transactionManager.commit();
			return result;
		} catch (Exception e) {
			try {
				if (transactionManager.getCurrentTransaction() != null) {
					transactionManager.rollback();
				}
			} catch (SystemException se) {
				LOGGER.error("Could not rollback transaction", se);
			}
			if (e instanceof F4MException) {
				// Re-throw, if exception is already coming from us
				throw (F4MException) e;
			} else {
				// Wrap exception in F4M runtime exception
				throw new F4MFatalErrorException("Error executing workflow transaction", e);
			}
		}
	}

	/**
	 * Get first tasks that can be executed by user.
	 */
	private TaskInfo getFirstTasksThatCanBeExecutedByUser(String userId, List<String> userGroups, String taskId,
			String actionType, InternalTaskService taskService) {
		List<TaskInfo> tasks = getTasks(taskId, taskService, actionType);
		if (tasks != null) {
			Set<String> verifiedTasks = new HashSet<>();
			Optional<TaskInfo> firstAvailableTask = tasks.stream().filter(summary -> {
				Task task = taskService.getTaskById(summary.getId());
				if (isAvailable(task, userId, userGroups)) {
					return true;
				} else {
					verifiedTasks.add(summary.getName());
					return false;
				}
			}).findFirst();
			return firstAvailableTask.isPresent() ? firstAvailableTask.get() : null;
		} else {
			return null;
		}
	}

	/**
	 * Determine if task is available for user.
	 */
	private boolean isAvailable(Task task, String userId, List<String> userGroups) {
		Optional<OrganizationalEntity> potentialOwner = task.getPeopleAssignments().getPotentialOwners().stream()
				.filter(entity -> 
					(entity instanceof User) && StringUtils.equals(StringUtils.substringBefore(entity.getId(), SEPARATOR), userId) ||
					(entity instanceof Group) && userGroups != null && userGroups.contains(entity.getId()))
				.findFirst();
		return potentialOwner.isPresent();
	}

	/** 
	 * Get available task set for given task ID.
	 */
	private Set<String> getAvailableTasks(String taskId, InternalTaskService taskService) {
		List<TaskInfo> tasks = getTasks(taskId, taskService, null);
		if (tasks != null) {
			Set<String> availableTasks = new HashSet<>();
			tasks.forEach(task -> availableTasks.add(task.getName()));
			return availableTasks;
		} else {
			return Collections.emptySet();
		}
	}

	/** 
	 * Get available tasks for given task ID.
	 */
	private List<TaskInfo> getTasks(String taskIdValue, InternalTaskService taskService, String actionType) {
		return taskService.execute(new TaskCommand<List<TaskInfo>>() {
			private static final long serialVersionUID = 1L;

			@Override
			public List<TaskInfo> execute(org.kie.internal.command.Context context) {
				TaskPersistenceContext pc = ((TaskContext) context).getPersistenceContext();
				return pc.queryStringWithParametersInTransaction("SELECT DISTINCT "
						+ "new " + TaskInfo.class.getName() + "( "
						+ "    t.id, "
						+ "    t.name) "
						+ "FROM " + TaskImpl.class.getSimpleName() + " t, " 
						+ VariableInstanceLog.class.getSimpleName() + " l, "
						+ ProcessInstanceInfo.class.getSimpleName() + " p "
						+ "WHERE t.taskData.processInstanceId = l.processInstanceId"
						+ "  AND p.state = 1 "
						+ "  AND p.processInstanceId = t.taskData.processInstanceId "
						+ "  AND t.archived = 0 "
						+ "  AND (t.name = :taskName OR :taskName IS NULL) "
						+ "  AND t.taskData.status IN (:taskStatuses) "
						+ "  AND l.variableInstanceId = :paramName "
						+ "  AND l.value = :taskId "
						+ "ORDER BY t.id DESC", 
						pc.addParametersToMap(
								"taskName", actionType, 
								"taskId", taskIdValue, 
								"taskStatuses", Arrays.asList(Status.Ready, Status.Reserved), 
								"paramName", PARAM_TASK_ID), 
						ClassUtil.<List<TaskInfo>>castClass(List.class));
			}
			
		});
	}
		
	/**
	 * Construct user and tenant ID.
	 */
	private String getUserAndTenantId(String userId, String tenantId) {
		return userId + SEPARATOR + tenantId;
	}

	/**
	 * Gracefully close workflow runtime manager and transaction manager.
	 */
	public void close() {
		schedulerService.shutdown();
		manager.close();
		dataSource.close();
		entityManagerFactory.close();
		transactionManager.shutdown();
	}

	@Override
	public void onInjectorClosing() {
		close();
	}

	/**
	 * Extended sub-proces node instance, transferring taskId variable to sub-processes.
	 */
	public static class ExtendedSubProcessNodeInstance extends SubProcessNodeInstance {

		private static final long serialVersionUID = 6030261892659945078L;

		@Override
		protected SubProcessNode getSubProcessNode() {
			SubProcessNode node = super.getSubProcessNode();
			boolean alreadyAdded = false;
			for (DataAssociation assoc : node.getInAssociations()) {
				if (assoc != null && assoc.getSources() != null && ! assoc.getSources().isEmpty() && PARAM_TASK_ID.equals(assoc.getSources().get(0))) {
					alreadyAdded = true;
					break;
				}
			}
			if (! alreadyAdded) {
				node.addInAssociation(new DataAssociation(PARAM_TASK_ID, PARAM_TASK_ID, null, null));
			}
			return node;
		}
		
		@Override
	    public Context resolveContext(String contextId, Object param) {
	    	if (PARAM_TASK_ID.equals(param)) {
	    		return ((ContextContainer) ((Node) getContextContainer()).getNodeContainer()).getDefaultContext(contextId);
	    	} else {
	    		return super.resolveContext(contextId, param);
	    	}
	    }
	}

}
