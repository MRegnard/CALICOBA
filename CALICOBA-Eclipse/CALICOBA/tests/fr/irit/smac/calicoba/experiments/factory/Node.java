package fr.irit.smac.calicoba.experiments.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class Node {
  private final int branch;
  protected final List<Node> parentNodes;

  public Node(final int branch, final int parentsNumber) {
    this.branch = branch;
    this.parentNodes = new ArrayList<>(parentsNumber);
    for (int i = 0; i < parentsNumber; i++) {
      this.parentNodes.add(null);
    }
  }

  public int getBranch() {
    return this.branch;
  }

  public boolean isParentSet(final int index) {
    return this.parentNodes.get(index) != null;
  }

  public void setParentNode(final int index, final Node node) {
    Objects.requireNonNull(node);
    if (node.getBranch() != -1 && node.getBranch() != this.branch) {
      throw new IllegalArgumentException(
          String.format("invalid branch for parent node, expected %d, got %d", this.branch, node.getBranch()));
    }
    this.parentNodes.set(index, node);
  }

  public int getInputsNumber() {
    return this.parentNodes.size();
  }

  public double compute() {
    if (this.parentNodes.stream().anyMatch(Objects::isNull)) {
      throw new IllegalStateException("missing parent node");
    }
    return this.computeImpl();
  }

  protected abstract double computeImpl();

  @Override
  public String toString() {
    return String.format("%s{%d}%s", this.getClass().getSimpleName(), this.branch, this.parentNodes);
  }
}
