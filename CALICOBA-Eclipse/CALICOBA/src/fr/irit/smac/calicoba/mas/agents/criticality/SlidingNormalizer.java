package fr.irit.smac.calicoba.mas.agents.criticality;

import java.util.Deque;
import java.util.LinkedList;

/**
 * A normalizer that uses the max of the last n passed values.
 *
 * @author Damien Vergnet
 */
public class SlidingNormalizer implements Normalizer {
  private Deque<Double> previousValues;
  private final int windowSize;

  public SlidingNormalizer(final int windowSize) {
    this.windowSize = windowSize;
    this.previousValues = new LinkedList<>();
  }

  @Override
  public double normalize(final double value) {
    this.previousValues.addFirst(Math.abs(value));
    if (this.previousValues.size() > this.windowSize) {
      this.previousValues.removeLast();
    }
    double max = this.previousValues.stream().mapToDouble(v -> v).max().getAsDouble();
    return max == 0 ? value : value / max;
  }
}
