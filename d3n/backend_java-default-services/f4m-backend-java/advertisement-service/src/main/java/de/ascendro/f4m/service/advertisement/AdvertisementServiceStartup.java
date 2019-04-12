package de.ascendro.f4m.service.advertisement;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.advertisement.AdvertisementMessageTypes;
import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.advertisement.di.AdvertisementServiceModule;
import de.ascendro.f4m.service.advertisement.di.AdvertisementServletModule;
import de.ascendro.f4m.service.advertisement.di.AdvertisementWebSocketModule;
import de.ascendro.f4m.service.usermessage.UserMessageMessageTypes;

public class AdvertisementServiceStartup extends ServiceStartup {
    public AdvertisementServiceStartup(Stage stage) {
        super(stage);
    }

    public static void main(String... args) throws Exception {
        new AdvertisementServiceStartup(DEFAULT_STAGE).start();
    }

    @Override
    public Injector createInjector(Stage stage) {
        return Guice.createInjector(Modules.override(super.getModules()).with(getModules()));
    }

    @Override
    protected String getServiceName() {
        return AdvertisementMessageTypes.SERVICE_NAME;
    }


    @Override
    protected Iterable<? extends Module> getModules() {
        return Arrays.asList(new AdvertisementServiceModule(), new AdvertisementWebSocketModule(), new AdvertisementServletModule());
    }

    @Override
    protected List<String> getDependentServiceNames() {
        return Arrays.asList(UserMessageMessageTypes.SERVICE_NAME);
    }
}
