package de.ascendro.f4m.service.workflow.integration;

import java.util.Arrays;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.workflow.WorkflowServiceStartup;

public class WorkflowServiceStartupUsingAerospikeMock extends WorkflowServiceStartup {
    public WorkflowServiceStartupUsingAerospikeMock(Stage stage) {
        super(stage);
    }

    @Override
    public Injector createInjector(Stage stage) {
        final Module superModule = Modules.override(getBaseModules()).with(super.getModules());
        return Guice.createInjector(Modules.override(superModule).with(getModules()));
    }

    @Override
    protected Iterable<? extends Module> getModules() {
        return Arrays.asList(getProfileServiceAerospikeOverrideModule());
    }

    protected AbstractModule getProfileServiceAerospikeOverrideModule() {
        return new AbstractModule() {

            @Override
            protected void configure() {
                bind(AerospikeClientProvider.class).to(AerospikeClientMockProvider.class).in(javax.inject.Singleton.class);
            }
        };
    }
}
