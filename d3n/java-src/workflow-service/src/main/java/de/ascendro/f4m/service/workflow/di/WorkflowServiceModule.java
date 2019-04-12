package de.ascendro.f4m.service.workflow.di;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.service.config.F4MConfigImpl;
import de.ascendro.f4m.service.workflow.config.WorkflowConfig;
import de.ascendro.f4m.service.workflow.utils.DefaultWorkflowResourceProvider;
import de.ascendro.f4m.service.workflow.utils.WorkflowResourceProvider;
import de.ascendro.f4m.service.workflow.utils.WorkflowWrapper;

public class WorkflowServiceModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(F4MConfigImpl.class).to(WorkflowConfig.class);
		bind(WorkflowConfig.class).in(Singleton.class);
		
		bind(WorkflowResourceProvider.class).to(DefaultWorkflowResourceProvider.class).in(Singleton.class);
		bind(WorkflowWrapper.class).in(Singleton.class);
        bind(AerospikeClientProvider.class).in(Singleton.class);
        bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);
	}

}
