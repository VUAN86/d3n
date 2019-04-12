package de.ascendro.f4m.service.friend.integration;

import static org.mockito.Mockito.mock;

import java.util.Arrays;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.server.AerospikeClientProvider;
import de.ascendro.f4m.server.analytics.tracker.Tracker;
import de.ascendro.f4m.server.profile.ApplicationConfigurationAerospikeDao;
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.friend.FriendManagerServiceStartup;

public class FriendManagerServiceStartupUsingAerospikeMock extends FriendManagerServiceStartup {

	public FriendManagerServiceStartupUsingAerospikeMock(Stage stage) {
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
				bind(AerospikeClientProvider.class).to(AerospikeClientMockProvider.class).in(Singleton.class);
				ApplicationConfigurationAerospikeDao appConfigDao = mock(ApplicationConfigurationAerospikeDao.class);
				bind(ApplicationConfigurationAerospikeDao.class).toInstance(appConfigDao);
				Tracker tracker = mock(Tracker.class);
				bind(Tracker.class).toInstance(tracker);
			}
		};
	}
}
