package de.ascendro.f4m.service.di;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

public class ServiceContextListener extends GuiceServletContextListener {
	public static final Injector INJECTOR = Guice.createInjector(new ServiceModule());

	@Override
	protected Injector getInjector() {
		return INJECTOR;
	}
}
