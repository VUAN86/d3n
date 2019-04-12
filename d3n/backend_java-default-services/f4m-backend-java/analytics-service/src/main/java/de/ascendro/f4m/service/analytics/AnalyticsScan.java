package de.ascendro.f4m.service.analytics;


public interface AnalyticsScan {
    void execute();
    void suspend();
    void resume();
    void shutdown();
    boolean isRunning();
}
