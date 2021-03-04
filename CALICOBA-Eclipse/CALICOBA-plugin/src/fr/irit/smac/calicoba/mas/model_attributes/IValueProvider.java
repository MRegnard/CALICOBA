package fr.irit.smac.calicoba.mas.model_attributes;

/**
 * A value source is an object that can provide values and whose value may be
 * set.
 * 
 * @author Damien Vergnet
 *
 * @param <T> The type of the returned values.
 */
public interface IValueProvider<T> {
  /**
   * @return A value.
   */
  T getValue();

  /**
   * Sets the current value of this source.
   * 
   * @param value The new value.
   * @note This operation may not be available for all sources.
   */
  default void setValue(T value) {
  }
}
