package de.ascendro.f4m.spark.analytics;


public enum StreamStatus {
    RUNNING("Stream is running"),
    FILE_MISSING("File is missing");

    private final String fullName;

    private StreamStatus(String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }
}
