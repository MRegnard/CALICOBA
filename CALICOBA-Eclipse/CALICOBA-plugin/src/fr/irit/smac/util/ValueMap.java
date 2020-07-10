package fr.irit.smac.util;

import java.util.HashMap;
import java.util.Map;

public class ValueMap extends HashMap<String, Double> {
  private static final long serialVersionUID = -912062128210552052L;

  public ValueMap() {
    super();
  }

  public ValueMap(int initialCapacity) {
    super(initialCapacity);
  }

  public ValueMap(Map<? extends String, ? extends Double> m) {
    super(m);
  }

  public ValueMap(int initialCapacity, float loadFactor) {
    super(initialCapacity, loadFactor);
  }

  @Override
  public ValueMap clone() {
    return (ValueMap) super.clone();
  }
}
