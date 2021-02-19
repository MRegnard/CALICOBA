package fr.irit.smac.calicoba.mas.agents.data;

/**
 * This class abstracts a variation direction.
 * 
 * @author Damien Vergnet
 */
public class Way {
  private final boolean increase;

  /**
   * Creates a new Way object.
   * 
   * @param increase If true, represents a value increase, otherwise a value
   *                 decrease.
   */
  public Way(boolean increase) {
    this.increase = increase;
  }

  /**
   * @return True if this object represents a value increase, false otherwise.
   */
  public boolean increase() {
    return this.increase;
  }

  /**
   * @return True if this object represents a value decrease, false otherwise.
   */
  public boolean decrease() {
    return !this.increase;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Way && this.increase == ((Way) obj).increase;
  }

  @Override
  public String toString() {
    return this.increase ? "+" : "-";
  }
}
