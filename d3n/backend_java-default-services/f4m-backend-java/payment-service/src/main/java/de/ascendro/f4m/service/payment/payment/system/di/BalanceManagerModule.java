package de.ascendro.f4m.service.payment.payment.system.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.payment.config.PaymentConfig;
import de.ascendro.f4m.service.payment.payment.system.manager.AccountBalanceManager;
import de.ascendro.f4m.service.payment.payment.system.manager.GameBalanceManager;
import de.ascendro.f4m.service.payment.payment.system.manager.impl.AccountBalanceManagerImpl;
import de.ascendro.f4m.service.payment.payment.system.manager.impl.GameBalanceManagerImpl;
import de.ascendro.f4m.service.payment.payment.system.repository.*;
import de.ascendro.f4m.service.payment.payment.system.repository.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;

public class BalanceManagerModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(BalanceManagerModule.class);

    private Config p = new PaymentConfig();

    @Override
    protected void configure() {
        bind(AccountBalanceRepository.class).to(AccountBalanceRepositoryImpl.class).in(Singleton.class);
        bind(CurrenciesRepository.class).to(CurrenciesRepositoryImpl.class).in(Singleton.class);
        bind(TenantRepository.class).to(TenantRepositoryImpl.class).in(Singleton.class);
        bind(TenantBalanceRepository.class).to(TenantBalanceRepositoryImpl.class).in(Singleton.class);
        bind(GameBalanceRepository.class).to(GameBalanceRepositoryImpl.class).in(Singleton.class);
        bind(GameBalanceManager.class).to(GameBalanceManagerImpl.class).in(Singleton.class);
        bind(AccountBalanceManager.class).to(AccountBalanceManagerImpl.class).in(Singleton.class);
        bind(TransactionRepository.class).to(TransactionRepositoryImpl.class).in(Singleton.class);
    }

    private static final ThreadLocal<EntityManager> ENTITY_MANAGER_CACHE
            = new ThreadLocal<>();

    @Provides
    @Singleton
    public EntityManagerFactory provideEntityManagerFactory() {
        Map<String, String> properties = new HashMap<>();
        properties.put("hibernate.connection.driver_class", p.getProperty(PaymentConfig.HIBERNATE_CONNECTION_DRIVER_CLASS));
        properties.put("hibernate.connection.url", p.getProperty(PaymentConfig.HIBERNATE_CONNECTION_URL));
        properties.put("hibernate.connection.username", p.getProperty(PaymentConfig.HIBERNATE_CONNECTION_USERNAME));
        properties.put("hibernate.connection.password", p.getProperty(PaymentConfig.HIBERNATE_CONNECTION_PASSWORD));
        properties.put("hibernate.connection.pool_size", p.getProperty(PaymentConfig.HIBERNATE_CONNECTION_POOL_SIZE));
        properties.put("hibernate.dialect", p.getProperty(PaymentConfig.HIBERNATE_DIALECT));
        LOGGER.debug("provideEntityManagerFactory {}", properties);
        return Persistence.createEntityManagerFactory("db-manager", properties);
    }

    @Provides
    public EntityManager provideEntityManager(EntityManagerFactory entityManagerFactory) {
        EntityManager entityManager = ENTITY_MANAGER_CACHE.get();
        if (entityManager == null) {
            ENTITY_MANAGER_CACHE.set(entityManager = entityManagerFactory.createEntityManager());
        }
        return entityManager;
    }
}
