package de.ascendro.f4m.service.winning.integration;

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
import de.ascendro.f4m.service.dao.aerospike.AerospikeClientMockProvider;
import de.ascendro.f4m.service.winning.WinningServiceStartup;
import de.ascendro.f4m.service.winning.dao.UserWinningComponentAerospikeDao;

public class WinningServiceStartupUsingAerospikeMock extends WinningServiceStartup {

	public WinningServiceStartupUsingAerospikeMock(Stage stage) {
		super(stage);
	}

	@Override
	public Injector createInjector(Stage stage) {
		final Module superModule = Modules.override(getBaseModules()).with(super.getModules());
		return Guice.createInjector(Modules.override(superModule).with(getModules()));
	}

	@Override
	protected Iterable<? extends Module> getModules() {
		return Arrays.asList(getWinningServiceAerospikeOverrideModule());
	}

	protected AbstractModule getWinningServiceAerospikeOverrideModule() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(AerospikeClientProvider.class).to(AerospikeClientMockProvider.class).in(Singleton.class);
				UserWinningComponentAerospikeDao userWinningComponentAerospikeDao = mock(UserWinningComponentAerospikeDao.class);
				bind(UserWinningComponentAerospikeDao.class).toInstance(userWinningComponentAerospikeDao);
			}
		};
	}
	
}
