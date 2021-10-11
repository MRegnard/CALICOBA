package fr.irit.smac.calicoba.test_util;

import java.util.Map;
import java.util.StringJoiner;

public final class Utils {
  public static String mapToString(Map<String, Double> m) {
    StringJoiner sj = new StringJoiner(";");
    for (Map.Entry<String, Double> e : m.entrySet()) {
      sj.add(e.getKey() + "=" + e.getValue());
    }
    return sj.toString();
  }

  private Utils() {
  }
}
