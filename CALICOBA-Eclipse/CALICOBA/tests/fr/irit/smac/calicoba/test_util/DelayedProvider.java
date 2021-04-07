package fr.irit.smac.calicoba.test_util;

import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Function;

public class DelayedProvider extends DummyValueProvider {
  private final Function<Double, Double> function;
  private final int delay;
  private Deque<Integer> remainingTimes;
  private Deque<Double> values;

  public DelayedProvider(double initialValue, int delay) {
    this(initialValue, delay, Function.identity());
  }

  public DelayedProvider(double initialValue, int delay, Function<Double, Double> f) {
    super(initialValue);
    this.delay = delay;
    this.function = f;
    this.remainingTimes = new LinkedList<>();
    this.values = new LinkedList<>();
  }

  public void step() {
    if (!this.remainingTimes.isEmpty()) {
      if (this.remainingTimes.peek() <= 0) {
        this.remainingTimes.poll();
        super.set(this.values.poll());
      } else {
        this.remainingTimes.push(this.remainingTimes.poll() - 1);
      }
    }
  }

  @Override
  public void set(Double value) {
    this.remainingTimes.add(this.delay);
    this.values.add(this.function.apply(value));
  }
}
