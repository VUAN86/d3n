package de.ascendro.f4m.service.result.engine.integration;

import java.util.Arrays;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.util.Modules;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.result.engine.ResultEngineServiceStartup;

public class ResultEngineServiceStartupUsingAerospikeMock extends ResultEngineServiceStartup {
    public ResultEngineServiceStartupUsingAerospikeMock(Stage stage) {
        super(stage);
    }

    @Override
    public Injector createInjector(Stage stage) {
        final Module superModule = Modules.override(getBaseModules()).with(super.getModules());
        return Guice.createInjector(Modules.override(superModule).with(getModules()));
    }

    @Override
    protected Iterable<? extends Module> getModules() {
        return Arrays.asList(getResultEngineAerospikeOverrideModule(), getModule());
    }

    protected void bindAdditional(ResultEngineMockModule module) {
        //Override if additional bindings is required
    }

    protected AbstractModule getResultEngineAerospikeOverrideModule() {
        return new ResultEngineMockModule();
    }

    protected AbstractModule getModule() {
    	return new ResultEngineMockModule() {
			@Override
			protected void configure() {
			}
    	};
    }

    protected class ResultEngineMockModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(AerospikeClientProvider.class).to(AerospikeClientMockProvider.class).in(Singleton.class);
            bindAdditional(this);
        }

        public <T> AnnotatedBindingBuilder<T> bindToModule(Class<T> clazz) {
            return this.bind(clazz);
        }
    }
}
