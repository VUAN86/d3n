package de.ascendro.f4m.service.analytics.util;

@FunctionalInterface
public interface FailingRunnable {
    void run() throws Exception;
}
