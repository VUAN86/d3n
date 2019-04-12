package de.ascendro.f4m.service.advertisement.integration;

import static org.mockito.Mockito.mock;

import java.util.Arrays;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.service.advertisement.AdvertisementServiceStartup;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;

public class AdvertisementServiceStartupUsingAerospikeMock extends AdvertisementServiceStartup {


    public AdvertisementServiceStartupUsingAerospikeMock(Stage stage) {
        super(stage);
    }

    @Override
    public Injector createInjector(Stage stage) {
        final Module superModule = Modules.override(getBaseModules()).with(super.getModules());
        return Guice.createInjector(Modules.override(superModule).with(getModules()));
    }

    @Override
    protected Iterable<? extends Module> getModules() {
        return Arrays.asList(getAdvertisementServiceAerospikeOverrideModule());
    }

    protected AbstractModule getAdvertisementServiceAerospikeOverrideModule() {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(AerospikeClientProvider.class).to(AerospikeClientMockProvider.class).in(Singleton.class);
                ApplicationConfigurationAerospikeDao appConfigDao = mock(ApplicationConfigurationAerospikeDao.class);
                bind(ApplicationConfigurationAerospikeDao.class).toInstance(appConfigDao);
            }
        };

    }

}
