package fr.irit.smac.util;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Objects;

/**
 * A very basic logger.
 * 
 * @author Damien Vergnet
 */
public final class Logger {
  /** Minimum level of events that will be logged by the standard output. */
  private static Level stdoutLevel = Level.DEBUG;
  /** Minimum level of events that will be logged by the writer. */
  private static Level writerLevel = Level.INFO;

  private static PrintStream printer = System.out;

  private static OutputStreamWriter writer;

  public static void setPrinter(PrintStream out) {
    printer = Objects.requireNonNull(out);
  }

  public static void setWriter(OutputStreamWriter out) {
    writer = out;
  }

  /**
   * Sets the minimum logging level.
   * 
   * @param level The minimum level.
   */
  public static void setStdoutLevel(Level level) {
    Logger.stdoutLevel = level;
  }

  /**
   * Sets the minimum logging level for the writer.
   * 
   * @param level The minimum level.
   */
  public static void setWriterLevel(Level level) {
    Logger.writerLevel = level;
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
    if (level.isHigherOrEqualTo(Logger.stdoutLevel)) {
      StackTraceElement[] trace = Thread.currentThread().getStackTrace();
      StackTraceElement callingMethodTrace = trace[3]; // Trace element for the calling method.

      String v = String.format("[%s] %s.%s:%d: %s", //
          Objects.requireNonNull(level), //
          callingMethodTrace.getClassName(), //
          callingMethodTrace.getMethodName(), //
          callingMethodTrace.getLineNumber(), //
          String.valueOf(value) //
      );

      printer.println(v);

      if (writer != null && level.isHigherOrEqualTo(writerLevel)) {
        try {
          writer.write(v + "\n");
          writer.flush();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
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
