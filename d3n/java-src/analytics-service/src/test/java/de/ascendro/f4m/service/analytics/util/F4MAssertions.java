package de.ascendro.f4m.service.analytics.util;

public class F4MAssertions {
    public static void assertDoesNotThrow(FailingRunnable action) {
        try {
            action.run();
        } catch (Exception ex) {
            throw new Error("Expected action not to throw, but it did!", ex);
        }
    }
}

