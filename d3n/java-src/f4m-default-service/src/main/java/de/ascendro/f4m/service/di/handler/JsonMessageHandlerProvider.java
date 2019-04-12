package de.ascendro.f4m.service.di.handler;


/**
 *  Binding class for business JSON message handler providers.
 *  Used as intermediate level between MessageHandlerProvider<String> and business bindings, which are done within 
 *  particular service implementation.
 */
public interface JsonMessageHandlerProvider extends MessageHandlerProvider<String>{
}
