package fr.irit.smac.util;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class CsvFileWriter extends FileWriter {
  private final String[] header;

  public CsvFileWriter(String fileName, boolean append, boolean writeHeader, String... header) throws IOException {
    super(fileName, append);
    this.header = header;
    if (writeHeader) {
      this.writeLine(this.header);
      this.flush();
    }
  }

  public void writeLine(int... values) throws IOException {
    this.writeLine(Arrays.stream(values).mapToObj(String::valueOf).toArray(String[]::new));
  }

  public void writeLine(long... values) throws IOException {
    this.writeLine(Arrays.stream(values).mapToObj(String::valueOf).toArray(String[]::new));
  }

  public void writeLine(double... values) throws IOException {
    this.writeLine(Arrays.stream(values).mapToObj(String::valueOf).toArray(String[]::new));
  }

  public void writeLine(Object... values) throws IOException {
    this.writeLine(Arrays.stream(values).map(String::valueOf).toArray(String[]::new));
  }

  public void writeLine(String... line) throws IOException {
    if (line.length != this.header.length) {
      throw new IllegalArgumentException("line should be the same length as header");
    }
    this.write(String.join(",", line) + "\n");
  }
}
