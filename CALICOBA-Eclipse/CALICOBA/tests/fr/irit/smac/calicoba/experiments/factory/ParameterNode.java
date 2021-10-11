package fr.irit.smac.calicoba.experiments.factory;

public class ParameterNode extends Node {
  private final String name;
  private double value;
  private int branchingFactor;

  public ParameterNode(final String name, int initialBranchingFactor) {
    super(-1, 0);
    this.name = name;
    this.branchingFactor = initialBranchingFactor;
  }

  public String getName() {
    return this.name;
  }

  public void setValue(double value) {
    this.value = value;
  }

  public int getBranchingFactor() {
    return this.branchingFactor;
  }

  public void decreaseBranchingFactor() {
    if (this.branchingFactor == 0) {
      throw new IllegalStateException("branching factor is 0");
    }
    this.branchingFactor--;
  }

  @Override
  protected double computeImpl() {
    return this.value;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
