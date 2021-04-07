package fr.irit.smac.calicoba.mas.model_attributes;

/**
 * A value provider and setter is an object that can provide values and whose
 * value may be set.
 * 
 * @author Damien Vergnet
 *
 * @param <T> The type of the returned and set values.
 */
public interface IValueProviderSetter<T> extends IValueProvider<T> {
  /**
   * Sets the current value of this source.
   * 
   * @param value The new value.
   */
  void set(T value);
}
