package fr.irit.smac.util;

public class Pair<K, V> {
  private final K key;
  private final V value;

  public Pair(K key, V value) {
    super();
    this.key = key;
    this.value = value;
  }

  public K getKey() {
    return this.key;
  }

  public V getValue() {
    return this.value;
  }

  public K getFirst() {
    return this.key;
  }

  public V getSecond() {
    return this.value;
  }
}
