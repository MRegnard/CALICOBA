package fr.irit.smac.calicoba.test_util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import fr.irit.smac.util.FixedCapacityQueue;
import fr.irit.smac.util.Pair;

public class PeriodDetector {
  private FixedCapacityQueue<Double> buffer;
  private List<Pair<Double, Integer>> patternsCache;
  private boolean needsUpdating;

  public PeriodDetector(final int bufferSize) {
    this.buffer = new FixedCapacityQueue<>(bufferSize);
    this.needsUpdating = true;
  }

  public void add(final double value) {
    this.buffer.add(value);
    this.needsUpdating = true;
  }

  public boolean isFull() {
    return this.buffer.isFull();
  }

  public Stream<Double> stream() {
    return this.buffer.stream();
  }

  public boolean hasConverged() {
    return this.getPeriodDistances().stream().anyMatch(p -> p.getFirst() < 1e-5);
  }

  /**
   * Tries to find the period of the values in the buffer.
   * 
   * @return A list of distance/period pairs.
   * @author Otunba, R. and Lin, J., 2014. APT: Approximate Period Detection in
   *         Time Series. In SEKE (pp. 490-494).
   */
  public List<Pair<Double, Integer>> getPeriodDistances() {
    if (!this.needsUpdating) {
      return this.patternsCache;
    }

    List<Double> series = new ArrayList<>(this.buffer);
    List<Pair<Double, Integer>> patterns = new LinkedList<>();
    double dist = Double.POSITIVE_INFINITY;
    int period = 2;

    for (int i = 2; i <= series.size() / 2; i++) {
      double cDist = euclidianDistance(series, i, dist);
      if (cDist < dist) {
        dist = cDist;
        if (i == period + 1) {
          patterns.remove(patterns.size() - 1);
        }
        period = i;
        patterns.add(new Pair<>(dist, period));
      }
    }

    patterns.sort((a, b) -> a.getFirst().compareTo(b.getFirst()));

    this.patternsCache = patterns;
    this.needsUpdating = false;

    return patterns;
  }

  /**
   * Computes the euclidian distance of the given series to a generated series of
   * the given period. The second series is generated using the first
   * {@code period} elements of the given series.
   * 
   * @param series          Series to use.
   * @param period          Period of the series to generate.
   * @param currentDistance Current minimal distance for early stopping.
   * @return The euclidian distance.
   * @author Otunba, R. and Lin, J., 2014. APT: Approximate Period Detection in
   *         Time Series. In SEKE (pp. 490-494).
   */
  private static double euclidianDistance(List<Double> series, int period, double currentDistance) {
    double newDist = 0;
    for (int j = 0; j < series.size(); j++) {
      newDist += Math.pow(series.get(j) - series.get(j % period), 2);
      if (newDist >= currentDistance * currentDistance) { // Optimisation
        return currentDistance;
      }
    }
    return Math.sqrt(newDist);
  }
}
