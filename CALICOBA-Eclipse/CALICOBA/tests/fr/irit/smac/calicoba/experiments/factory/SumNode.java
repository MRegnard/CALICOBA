package fr.irit.smac.calicoba.experiments.factory;

public class SumNode extends BiNode {
  public SumNode(final int branch) {
    super(branch, "+");
  }

  @Override
  protected double computeImpl() {
    return this.parentNodes.stream().mapToDouble(Node::compute).sum();
  }
}
