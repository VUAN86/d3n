package de.ascendro.f4m.service.analytics.di;

import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.module.sparc.SparkProcessor;
import de.ascendro.f4m.service.analytics.module.sparc.advisory.ConsumerAdvisoryListener;
import de.ascendro.f4m.service.analytics.module.sparc.advisory.NoConsumerAdvisoryListener;


public class SparkModule extends BaseConsumerModule {

    @Override
    protected void configure() {
        bind(ConsumerAdvisoryListener.class).in(Singleton.class);
        bind(NoConsumerAdvisoryListener.class).in(Singleton.class);

        Multibinder<AnalyticMessageListener> messageHandlers = Multibinder.newSetBinder(binder(), AnalyticMessageListener.class);
        messageHandlers.addBinding().to(SparkProcessor.class).in(Scopes.SINGLETON);
    }
}
