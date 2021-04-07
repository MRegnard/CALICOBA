package fr.irit.smac.calicoba.mas.model_attributes;

/**
 * This class represents an attribute of the target model whose value can be
 * read.
 *
 * @author Damien Vergnet
 * 
 * @param <T> The value type of this attribute.
 */
public class ReadableModelAttribute<T, P extends IValueProvider<T>> {
  /** This attribute’s name. */
  private final String name;
  /** The attribute. */
  protected final P valueProvider;
  /** The lowest allowed value for the attribute. */
  protected final double min;
  /** The highest allowed value for the attribute. */
  protected final double max;

  /**
   * Creates a readable attribute for the given provider.
   *
   * @param valueProvider The value provider.
   * @param name          The name of the attribute represented by this object.
   * @param min           The lowest allowed value for the attribute.
   * @param max           The highest allowed value for the attribute.
   */
  public ReadableModelAttribute(final P valueProvider, final String name, final double min, final double max) {
    this.valueProvider = valueProvider;
    this.name = name;
    this.min = min;
    this.max = max;
  }

  /**
   * @return This attribute’s name.
   */
  public String getName() {
    return this.name;
  }

  /**
   * @return This attribute’s value.
   */
  public T getValue() {
    return this.valueProvider.get();
  }

  /**
   * @return The minimum allowed value.
   */
  public double getMin() {
    return this.min;
  }

  /**
   * @return The maximum allowed value.
   */
  public double getMax() {
    return this.max;
  }

  @Override
  public String toString() {
    return String.format("RModelAttr{name=%s,value=%s}", this.getName(), this.getValue());
  }
}
