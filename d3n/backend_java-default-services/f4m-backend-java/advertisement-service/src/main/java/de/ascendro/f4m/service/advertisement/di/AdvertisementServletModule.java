package de.ascendro.f4m.service.advertisement.di;


import javax.inject.Singleton;

import com.google.inject.servlet.ServletModule;

import de.ascendro.f4m.service.advertisement.config.AdvertisementConfig;
import de.ascendro.f4m.service.advertisement.server.FyberEndpointCallback;

public class AdvertisementServletModule extends ServletModule {

    @Override
    protected void configureServlets() {
        bind(FyberEndpointCallback.class).in(Singleton.class);

        serve(AdvertisementConfig.FYBER_ENDPOINT_CONTEXT_PATH).with(FyberEndpointCallback.class);
    }
}
