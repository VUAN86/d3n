package de.ascendro.f4m.service.di.handler;

import de.ascendro.f4m.service.handler.F4MMessageHandler;

public interface MessageHandlerProvider<E> {
	F4MMessageHandler<E> get();
}
