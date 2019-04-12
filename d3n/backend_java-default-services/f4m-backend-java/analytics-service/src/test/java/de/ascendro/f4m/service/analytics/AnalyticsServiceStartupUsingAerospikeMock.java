package de.ascendro.f4m.service.analytics;

import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.util.Modules;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.analytics.listeners.TestSparkListener;
import de.ascendro.f4m.service.analytics.module.notification.TestNotificationListener;
import de.ascendro.f4m.service.analytics.module.statistic.TestStatisticListener;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;

public class AnalyticsServiceStartupUsingAerospikeMock extends AnalyticsServiceStartup {

    public AnalyticsServiceStartupUsingAerospikeMock(Stage stage) {
        super(stage);
    }

    @Override
    public Injector createInjector(Stage stage) {
        final Module superModule = Modules.override(getBaseModules()).with(getTestedModules());
        return Guice.createInjector(stage, Modules.override(superModule).with(getModules()));
    }

    @Override
    protected Iterable<? extends Module> getModules() {
        return Collections.singletonList(getAnalyticsServiceAerospikeOverrideModule());
    }

    protected Iterable<? extends Module> getTestedModules() {
        return super.getModules();
    }

    protected List<Class<? extends AnalyticMessageListener>> getTestListeners() {
		return Arrays.asList(TestStatisticListener.class, TestNotificationListener.class, TestSparkListener.class);
    }

    protected AbstractModule getAnalyticsServiceAerospikeOverrideModule() {
        return new AbstractModule() {
            @Override
            protected void configure() {
                Multibinder<AnalyticMessageListener> messageHandlers = Multibinder.newSetBinder(binder(), AnalyticMessageListener.class);
				for (Class<? extends AnalyticMessageListener> listenerClass : getTestListeners()) {
                    messageHandlers.addBinding().to(listenerClass).in(Scopes.SINGLETON);
                }

                ApplicationConfigurationAerospikeDao appConfigDao = mock(ApplicationConfigurationAerospikeDao.class);
                bind(ApplicationConfigurationAerospikeDao.class).toInstance(appConfigDao);

                bind(AerospikeClientProvider.class).to(AerospikeClientMockProvider.class).in(Singleton.class);
                bind(AnalyticsConfig.class).to(AnalyticsTestConfig.class).in(Singleton.class);
                bind(AnalyticsTestConfig.class).in(Singleton.class);
            }
        };
    }
}