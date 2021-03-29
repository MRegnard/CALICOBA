package fr.irit.smac.calicoba.mas.model_attributes;

/**
 * This class represents an attribute of the target model whose value can be
 * read and set.
 *
 * @author Damien Vergnet
 * 
 * @param <T> The value type of this attribute.
 */
public class WritableModelAttribute<T, P extends IValueProviderSetter<T>> extends ReadableModelAttribute<T, P> {
  /**
   * Creates a writable attribute for the given provider.
   *
   * @param valueProvider The value provider and setter.
   * @param name          The name of the attribute represented by this object.
   * @param min           The lowest allowed value for the attribute.
   * @param max           The highest allowed value for the attribute.
   */
  public WritableModelAttribute(P valueProvider, final String name, final double min, final double max) {
    super(valueProvider, name, min, max);
  }

  /**
   * Sets the value of this attribute.
   *
   * @param value The new value.
   */
  public void setValue(T value) {
    this.valueProvider.setValue(value);
  }

  @Override
  public String toString() {
    return "W" + super.toString();
  }
}
