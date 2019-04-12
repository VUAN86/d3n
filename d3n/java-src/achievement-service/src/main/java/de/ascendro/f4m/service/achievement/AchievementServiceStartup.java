package de.ascendro.f4m.service.achievement;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.util.Modules;

import de.ascendro.f4m.service.ServiceStartup;
import de.ascendro.f4m.service.event.EventMessageTypes;
import de.ascendro.f4m.service.achievement.di.AchievementServiceModule;
import de.ascendro.f4m.service.achievement.di.AchievementWebSocketModule;

public class AchievementServiceStartup extends ServiceStartup {

    public AchievementServiceStartup(Stage stage) {
        super(stage);
    }

    public static void main(String... args) throws Exception {
        new AchievementServiceStartup(DEFAULT_STAGE).startK8S();
    }

    @Override
    public Injector createInjector(Stage stage) {
        return Guice.createInjector(Modules.override(super.getModules()).with(getModules()));
    }

    @Override
    protected List<String> getDependentServiceNames() {
        return Arrays.asList(EventMessageTypes.SERVICE_NAME);
    }

    @Override
    protected Iterable<? extends Module> getModules() {
        return Arrays.asList(new AchievementServiceModule());
    }


    @Override
    protected String getServiceName() {
        return AchievementMessageTypes.SERVICE_NAME;
    }
}
