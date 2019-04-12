package de.ascendro.f4m.service.analytics;

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

import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.config.AnalyticsConfig;
import de.ascendro.f4m.service.analytics.listeners.TestSparkListener;
import de.ascendro.f4m.service.analytics.module.notification.TestNotificationListener;
import de.ascendro.f4m.service.analytics.module.statistic.TestStatisticListener;

public class AnalyticsServiceStartupUsingAerospikeReal extends AnalyticsServiceStartup {

    public AnalyticsServiceStartupUsingAerospikeReal(Stage stage) {
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
                for(Class<? extends AnalyticMessageListener> listenerClass: getTestListeners()) {
                    messageHandlers.addBinding().to(listenerClass).in(Scopes.SINGLETON);
                }
                bind(AnalyticsConfig.class).to(AnalyticsTestConfig.class).in(Singleton.class);
                bind(AnalyticsTestConfig.class).in(Singleton.class);
            }
        };
    }
}
