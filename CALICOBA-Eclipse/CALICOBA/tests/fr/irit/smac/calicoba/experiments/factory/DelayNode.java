package fr.irit.smac.calicoba.experiments.factory;

import java.util.Queue;

import fr.irit.smac.util.FixedCapacityQueue;

public class DelayNode extends Node {
  private Queue<Double> queue;

  public DelayNode(final int branch) {
    super(branch, 1);
    this.queue = new FixedCapacityQueue<>(30);
  }

  @Override
  protected double computeImpl() {
    this.queue.add(this.parentNodes.get(0).compute());
    return this.queue.stream().mapToDouble(v -> v).average().getAsDouble();
  }

  @Override
  public String toString() {
    return String.format("delay(%s)", this.parentNodes.get(0));
  }
}
