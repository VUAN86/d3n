package de.ascendro.f4m.spark.analytics;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class StreamCursor {
    private static final String STREAM_CURSOR_FILE_NAME = "stream_cursor.lck";

    private StreamCursor() {}

    public static synchronized void updateCursor(Long time) throws SparkApplicationException {
        try {
            if (Files.readAllLines(Paths.get(STREAM_CURSOR_FILE_NAME), StandardCharsets.UTF_8).size() > 1) {
                throw new SparkApplicationException("Cursor file is corrupt");
            }
            Files.write(Paths.get(STREAM_CURSOR_FILE_NAME), Arrays.asList(time.toString()), StandardCharsets.UTF_8);
        } catch (IOException ioe) {
            throw new SparkApplicationException("Error accessing cursor file", ioe);
        }
    }

    public static synchronized Long cursorValue() throws SparkApplicationException {
        try {
            File cursorFile = new File(STREAM_CURSOR_FILE_NAME);
            if (cursorFile.exists()) {
                List<String> oldLines = Files.readAllLines(Paths.get(STREAM_CURSOR_FILE_NAME), StandardCharsets.UTF_8);
                if (oldLines.size() != 1) {
                    throw new SparkApplicationException("Cursor file is corrupt");
                }

                return Long.parseLong(oldLines.get(0));
            } else {
                cursorFile.createNewFile();
                updateCursor(0L);
                return 0L;
            }
        } catch (IOException ioe) {
            throw new SparkApplicationException("Error accessing cursor file", ioe);
        }
    }
}
