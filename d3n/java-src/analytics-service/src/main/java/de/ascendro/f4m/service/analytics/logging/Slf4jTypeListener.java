package de.ascendro.f4m.service.analytics.logging;

import java.lang.reflect.Field;

import org.slf4j.Logger;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

public class Slf4jTypeListener implements TypeListener {

    @Override
	public <I> void hear(TypeLiteral<I> aTypeLiteral, TypeEncounter<I> aTypeEncounter) {
        Class<?> rawType = aTypeLiteral.getRawType();
        while (rawType != Object.class && rawType != null) {
            for (Field field : rawType.getDeclaredFields()) {
                if (field.getType() == Logger.class && field.isAnnotationPresent(InjectLogger.class)) {
                    aTypeEncounter.register(new Slf4jMembersInjector<>(field));

                }
            }
            rawType = rawType.getSuperclass();
        }
    }
}

