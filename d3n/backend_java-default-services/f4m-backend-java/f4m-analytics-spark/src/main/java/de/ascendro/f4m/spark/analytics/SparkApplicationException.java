package de.ascendro.f4m.spark.analytics;


public class SparkApplicationException extends Exception {
    static final long serialVersionUID = 1;

    public SparkApplicationException(String message) {
        super(message);
    }

    public SparkApplicationException(Exception e) {
        super(e);
    }

    public SparkApplicationException(String message, Exception e) {
        super(message, e);
    }
}
