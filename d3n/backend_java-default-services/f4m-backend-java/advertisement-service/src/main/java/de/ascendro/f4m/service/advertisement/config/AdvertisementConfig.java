package de.ascendro.f4m.service.advertisement.config;

import de.ascendro.f4m.service.config.F4MConfigImpl;

public class AdvertisementConfig extends F4MConfigImpl {

    public static final String CALLBACK_BASE_CONTEXT_PATH = "callbackContextPath";
    public static final String CALLBACK_BASE_CONTEXT_PATH_DEFAULT = "/callback";
    public static final String FYBER_ENDPOINT_CONTEXT_PATH = "/reward";

    public AdvertisementConfig(){
        setProperty(CALLBACK_BASE_CONTEXT_PATH, CALLBACK_BASE_CONTEXT_PATH_DEFAULT);
        loadProperties();
    }

    public String getCallbackBaseContextPath() {
        return this.getProperty(CALLBACK_BASE_CONTEXT_PATH);
    }
}
