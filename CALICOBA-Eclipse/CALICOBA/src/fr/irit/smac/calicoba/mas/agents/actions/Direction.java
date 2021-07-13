package fr.irit.smac.calicoba.mas.agents.actions;

/**
 * This enumeration represents variation directions.
 * 
 * @author Damien Vergnet
 */
public enum Direction {
  INCREASE('+', 1), DECREASE('-', -1), STAY('0', 0);

  public static Direction fromAction(int action) {
    switch (action) {
      case 1:
        return INCREASE;
      case -1:
        return DECREASE;
      default:
        return STAY;
    }
  }

  private final char c;
  private final int action;

  private Direction(char c, int action) {
    this.c = c;
    this.action = action;
  }

  public int getAction() {
    return this.action;
  }

  public Direction getOpposite() {
    switch (this) {
      case INCREASE:
        return DECREASE;
      case DECREASE:
        return INCREASE;
      case STAY:
        return STAY;
    }
    // Should never happen
    throw new Error("invalid enum value");
  }

  @Override
  public String toString() {
    return "" + this.c;
  }
}
