package fr.irit.smac.calicoba.mas.model_attributes;

/**
 * This class represents an attribute of the target model whose value can be
 * read and set.
 *
 * @author Damien Vergnet
 * 
 * @param <T> The value type of this attribute.
 */
public class WritableAgentAttribute<T> extends ReadableModelAttribute<T> {
  /**
   * Creates a writable attribute for the given provider.
   *
   * @param valueProvider The value provider.
   * @param name          The name of the attribute represented by this object.
   * @param min           The lowest allowed value for the attribute.
   * @param max           The highest allowed value for the attribute.
   */
  public WritableAgentAttribute(IValueProvider<T> valueProvider, final String name, final double min,
      final double max) {
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
