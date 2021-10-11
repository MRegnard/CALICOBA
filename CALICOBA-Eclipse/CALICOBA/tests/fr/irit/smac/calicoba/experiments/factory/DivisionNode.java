package fr.irit.smac.calicoba.experiments.factory;

public class DivisionNode extends BiNode {
  public DivisionNode(final int branch) {
    super(branch, "/");
  }

  @Override
  protected double computeImpl() {
    return this.parentNodes.get(0).compute() / this.parentNodes.get(1).compute();
  }
}
