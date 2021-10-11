package fr.irit.smac.calicoba.experiments.factory;

public abstract class BiNode extends Node {
  private final String operator;

  public BiNode(final int branch, final String operator) {
    super(branch, 2);
    this.operator = operator;
  }

  @Override
  public String toString() {
    return String.format("(%s %s %s)", this.parentNodes.get(0), this.operator, this.parentNodes.get(1));
  }
}
