package de.ascendro.f4m.service.session.pool;

import javax.inject.Inject;

import com.google.inject.assistedinject.Assisted;

import de.ascendro.f4m.service.config.Config;
import de.ascendro.f4m.service.logging.LoggingUtil;
import de.ascendro.f4m.service.session.SessionWrapper;

@FunctionalInterface
public interface SessionStoreCreator {
	@Inject
	SessionStore createSessionStore(Config config, LoggingUtil loggingUtil, @Assisted SessionWrapper sessionWrapper);
}
