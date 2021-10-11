package fr.irit.smac.calicoba.experiments.factory;

public class ProductNode extends BiNode {
  public ProductNode(final int branch) {
    super(branch, "×");
  }

  @Override
  protected double computeImpl() {
    return this.parentNodes.stream().mapToDouble(Node::compute).reduce(1, (a, b) -> a * b);
  }
}
