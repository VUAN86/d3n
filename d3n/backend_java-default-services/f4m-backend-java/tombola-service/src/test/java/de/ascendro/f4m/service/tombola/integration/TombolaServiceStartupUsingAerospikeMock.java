package de.ascendro.f4m.service.tombola.integration;

import static org.mockito.Mockito.mock;

import java.util.Collections;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.tombola.TombolaServiceStartup;

public class TombolaServiceStartupUsingAerospikeMock extends TombolaServiceStartup {
    TombolaServiceStartupUsingAerospikeMock(Stage stage) {
        super(stage);
    }

    @Override
    public Injector createInjector(Stage stage) {
        final Module superModule = Modules.override(getBaseModules()).with(super.getModules());
        return Guice.createInjector(Modules.override(superModule).with(getModules()));
    }

    @Override
    protected Iterable<? extends Module> getModules() {
        return Collections.singletonList(getTombolaServiceAerospikeOverrideModule());
    }

    private AbstractModule getTombolaServiceAerospikeOverrideModule() {
        return new AbstractModule() {

            @Override
            protected void configure() {
                bind(AerospikeClientProvider.class).to(AerospikeClientMockProvider.class).in(
                        javax.inject.Singleton.class);
                Tracker tracker = mock(Tracker.class);
                bind(Tracker.class).toInstance(tracker);
            }
        };
    }
}
