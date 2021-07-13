package fr.irit.smac.calicoba.scenarios;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

public class TestFunctions {
  private static final double π = Math.PI;

  public static double f1(double... values) {
    check(values);
    return Arrays.stream(values).map(x -> x * x).sum();
  }

  public static double f2(double... values) {
    check(values);
    double sum = Arrays.stream(values).map(Math::abs).sum();
    double prod = Arrays.stream(values).map(Math::abs).reduce(1, (x, y) -> x * y);
    return sum + prod;
  }

  public static double f3(double... values) {
    check(values);
    double res = 0;
    for (int i = 0; i < values.length; i++) {
      res += Math.pow(Arrays.stream(Arrays.copyOf(values, i + 1)).sum(), 2);
    }
    return res;
  }

  public static double f4(double... values) {
    check(values);
    return Arrays.stream(values).map(Math::abs).max().getAsDouble();
  }

  public static double f5(double... values) {
    check(values);
    double res = 0;
    for (int i = 0; i < values.length; i++) {
      res += i * Math.pow(values[i], 4);
    }
    return res + Math.random();
  }

  public static double f6(double... values) {
    check(values);
    Function<Double, Double> f = x -> x * x - 10 * Math.cos(2 * π * x) + 10;
    return 0; // XXX ?
  }

  public static double f7(double... values) {
    check(values);
    double c = 1.0 / values.length;
    double a = Arrays.stream(values).map(x -> x * x).sum();
    double b = Arrays.stream(values).map(x -> Math.cos(2 * π * x)).sum();
    return -20 * Math.exp(-0.2 * c * a) - Math.exp(c * b) + 20 + Math.E;
  }

  public static double f8(double... values) {
    check(values);
    double a = Arrays.stream(values).map(x -> x * x).sum();
    double b = 1;
    for (int i = 0; i < values.length; i++) {
      b *= Math.cos(values[i] / Math.sqrt(i));
    }
    return a / 4000 - b + 1;
  }

  public static double f9(double... values) {
    check(values);
    int n = values.length;
    double a = Arrays.stream(values).map(x -> x - 1).sum();
    double b = 0; // XXX ?
    double c = Arrays.stream(values).map(x -> u(x, 5, 100, 4)).sum();

    return (π / n) * (10 * Math.sin(π * values[1]) + a * (1 + 10 * Math.pow(Math.sin(π * b), 2))
        + Math.pow(values[values.length - 1] - 1, 2)) + c;
  }

  public static double f10(double... values) {
    check(values);
    DoubleUnaryOperator f = x -> Math.pow(x - 1, 2) * (1 + Math.pow(Math.sin(3 * π * x + 1), 2));
    double a = Arrays.stream(values).map(f).sum();
    double b = Arrays.stream(values).map(x -> u(x, 5, 100, 4)).sum();
    double c = values[values.length - 1];

    return 0.1
        * (Math.pow(Math.sin(3 * π * values[0]), 2) + a + Math.pow(c - 1, 2) * (1 + Math.pow(Math.sin(2 * π * c), 2)))
        + b;
  }

  private static double u(double x, int a, int k, int m) {
    if (x > a) {
      return k * Math.pow(x - a, m);
    } else if (x < -a) {
      return k * Math.pow(-x - a, m);
    } else {
      return 0;
    }
  }

  private static void check(double[] values) {
    Objects.requireNonNull(values);
    if (values.length == 0) {
      throw new IllegalArgumentException("array cannot be empty");
    }
  }
}
