package de.ascendro.f4m.service.analytics.di;

import javax.inject.Singleton;

import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;

import de.ascendro.f4m.server.profile.CommonProfileAerospikeDao;
import de.ascendro.f4m.server.profile.CommonProfileAerospikeDaoImpl;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDao;
import de.ascendro.f4m.server.transaction.log.TransactionLogAerospikeDaoImpl;
import de.ascendro.f4m.service.analytics.activemq.router.AnalyticMessageListener;
import de.ascendro.f4m.service.analytics.module.jobs.JobProcessor;
import de.ascendro.f4m.service.analytics.module.jobs.util.JobsUtil;

public class JobModule  extends BaseConsumerModule {

    @Override
    protected void configure() {
        bind(JobsUtil.class).in(Singleton.class);
        bind(TransactionLogAerospikeDao.class).to(TransactionLogAerospikeDaoImpl.class).in(Singleton.class);
        bind(CommonProfileAerospikeDao.class).to(CommonProfileAerospikeDaoImpl.class).in(Singleton.class);

        Multibinder<AnalyticMessageListener> messageHandlers = Multibinder.newSetBinder(binder(), AnalyticMessageListener.class);
        messageHandlers.addBinding().to(JobProcessor.class).in(Scopes.SINGLETON);
    }
}
