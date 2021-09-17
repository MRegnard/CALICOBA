package fr.irit.smac.util;

public class Triplet<V1, V2, V3> {
  private final V1 value1;
  private final V2 value2;
  private final V3 value3;

  public Triplet(V1 value1, V2 value2, V3 value3) {
    this.value1 = value1;
    this.value2 = value2;
    this.value3 = value3;
  }

  public V1 getFirst() {
    return this.value1;
  }

  public V2 getSecond() {
    return this.value2;
  }

  public V3 getThird() {
    return this.value3;
  }
}
