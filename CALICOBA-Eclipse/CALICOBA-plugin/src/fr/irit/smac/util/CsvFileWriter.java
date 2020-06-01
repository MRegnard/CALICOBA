package fr.irit.smac.util;

import java.io.FileWriter;
import java.io.IOException;

public class CsvFileWriter extends FileWriter {
  private final String[] header;

  public CsvFileWriter(String fileName, String... header) throws IOException {
    super(fileName);
    this.header = header;
  }

  public void writeLine(String... line) throws IOException {
    if (line.length != this.header.length) {
      throw new IllegalArgumentException("line should be the same length as header");
    }
    this.write(String.join(",", line));
  }
}
