package fr.irit.smac.util;

import java.util.Objects;

/**
 * Simple logger.
 * 
 * @author Damien Vergnet
 */
public final class Logger {
  /** Minimum level of events that will be logged. */
  private static Level level = Level.DEBUG;

  /**
   * Sets the minimum logging level.
   * 
   * @param level The minimum level.
   */
  public static void setLevel(Level level) {
    Logger.level = level;
  }

  /**
   * Logs a value with the Debug level.
   * 
   * @param value The value to log.
   */
  public static void debug(Object value) {
    log(Level.DEBUG, value);
  }

  /**
   * Logs a value with the Info level.
   * 
   * @param value The value to log.
   */
  public static void info(Object value) {
    log(Level.INFO, value);
  }

  /**
   * Logs a value with the Warning level.
   * 
   * @param value The value to log.
   */
  public static void warning(Object value) {
    log(Level.WARNING, value);
  }

  /**
   * Logs a value with the Error level.
   * 
   * @param value The value to log.
   */
  public static void error(Object value) {
    log(Level.ERROR, value);
  }

  /**
   * Logs a value if it the level is equal or above the current logger level.
   * 
   * @param level The logging level.
   * @param value The value to log.
   */
  private static void log(Level level, Object value) {
    if (level.isHigherOrEqualTo(Logger.level)) {
      StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      StackTraceElement callingMethodTrace = trace[3];

      System.out.println(String.format("[%s] %s.%s:%d: %s", //
          Objects.requireNonNull(level), //
          callingMethodTrace.getClassName(), //
          callingMethodTrace.getMethodName(), //
          callingMethodTrace.getLineNumber(), //
          String.valueOf(value) //
      ));
    }
  }

  private Logger() {
  }

  /**
   * Enumeration of logging levels.
   * 
   * @author Damien Vergnet
   */
  public enum Level {
    INFO("INFO", 1), WARNING("WARN", 2), ERROR("ERROR", 3), DEBUG("DEBUG", 0);

    private final String label;
    private final int severity;

    private Level(String label, int severity) {
      this.label = label;
      this.severity = severity;
    }

    /**
     * @return The label of this level.
     */
    public String getLabel() {
      return this.label;
    }

    /**
     * @return The severity of this level.
     */
    public int getSeverity() {
      return this.severity;
    }

    /**
     * Indicates wether this level has a severity greater or equal to the given
     * level.
     * 
     * @param other The other level.
     * @return True iff this level is higher than the argument.
     */
    public boolean isHigherOrEqualTo(Level other) {
      return this.getSeverity() >= other.getSeverity();
    }

    @Override
    public String toString() {
      return this.label;
    }
  }
}
