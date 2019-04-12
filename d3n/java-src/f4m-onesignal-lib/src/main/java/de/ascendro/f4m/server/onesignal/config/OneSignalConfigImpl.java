package de.ascendro.f4m.server.onesignal.config;

import de.ascendro.f4m.service.config.F4MConfig;

public class OneSignalConfigImpl extends F4MConfig {

    /**
     * Parameter name to specify OneSignal App ID
     */
    public static final String ONE_SIGNAL_APP_ID = "oneSignalAppId";

    /**
     * Parameter name to specify OneSignal App REST API Key
     */
    public static final String ONE_SIGNAL_APP_REST_API_KEY = "oneSignalAppRestApiKey";

    public static final String APP_ID_TAG = "appIdTag";
    public static final String PROFILE_ID_TAG = "profileIdTag";

    public OneSignalConfigImpl() {
        loadProperties();
    }
}
