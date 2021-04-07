package fr.irit.smac.calicoba.mas.model_attributes;

/**
 * A value provider is an object that can provide values.
 * 
 * @author Damien Vergnet
 *
 * @param <T> The type of the returned values.
 */
public interface IValueProvider<T> {
  /**
   * @return A value.
   */
  T get();
}
