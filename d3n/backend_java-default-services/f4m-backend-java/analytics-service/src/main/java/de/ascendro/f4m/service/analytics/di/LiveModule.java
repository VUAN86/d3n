package de.ascendro.f4m.service.analytics.di;

import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;

import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.module.livemap.LiveProcessor;

public class LiveModule extends BaseConsumerModule {

    @Override
    protected void configure() {
        Multibinder<AnalyticMessageListener> messageHandlers = Multibinder.newSetBinder(binder(), AnalyticMessageListener.class);
        messageHandlers.addBinding().to(LiveProcessor.class).in(Scopes.SINGLETON);
    }
}
