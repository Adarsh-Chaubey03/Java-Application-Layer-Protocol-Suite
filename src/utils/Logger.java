package utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple logging utility with level-based filtering.
 * Supports INFO, DEBUG, and ERROR levels.
 * Debug messages are only printed when verbose mode is enabled via Config.
 */
public class Logger {

    /** Timestamp format for log messages */
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** Log level enumeration */
    public enum Level {
        DEBUG, INFO, ERROR
    }

    /**
     * Log an INFO-level message.
     *
     * @param message the message to log
     */
    public static void info(String message) {
        log(Level.INFO, message);
    }

    /**
     * Log a DEBUG-level message (only visible in verbose mode).
     *
     * @param message the message to log
     */
    public static void debug(String message) {
        if (Config.isVerbose()) {
            log(Level.DEBUG, message);
        }
    }

    /**
     * Log an ERROR-level message.
     *
     * @param message the message to log
     */
    public static void error(String message) {
        log(Level.ERROR, message);
    }

    /**
     * Internal log method that formats and prints the message.
     *
     * @param level   the log level
     * @param message the message to log
     */
    private static void log(Level level, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String threadName = Thread.currentThread().getName();
        String formatted = String.format("[%s] [%s] [%s] %s",
                timestamp, level.name(), threadName, message);

        if (level == Level.ERROR) {
            System.err.println(formatted);
        } else {
            System.out.println(formatted);
        }
    }

    /**
     * Log a separator line for visual clarity.
     */
    public static void separator() {
        System.out.println("═".repeat(70));
    }

    /**
     * Log a section header.
     *
     * @param title the section title
     */
    public static void section(String title) {
        separator();
        System.out.println("  " + title);
        separator();
    }
}
