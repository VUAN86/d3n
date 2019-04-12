package de.ascendro.f4m.spark.analytics.log;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class ApplicationFileLogger {

    private ApplicationFileLogger() {}

    public static void initLogger(Level level) {
        Logger rootLogger = Logger.getRootLogger();
        rootLogger.setLevel(level);

        //change spark system log level
        Logger.getLogger("org").setLevel(Level.ERROR);
        Logger.getLogger("akka").setLevel(Level.ERROR);

        FileAppender fa = new FileAppender();
        fa.setName("ApplicationFileLogger");
        fa.setFile("F4M.log");
        fa.setLayout(new PatternLayout("%d %-5p [%c{1}] %m%n"));
        fa.setThreshold(level);
        fa.setAppend(true);
        fa.activateOptions();

        rootLogger.addAppender(fa);
    }
}
