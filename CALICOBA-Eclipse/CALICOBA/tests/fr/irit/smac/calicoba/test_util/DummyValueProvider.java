package fr.irit.smac.calicoba.test_util;

import fr.irit.smac.calicoba.mas.model_attributes.IValueProviderSetter;

public class DummyValueProvider implements IValueProviderSetter<Double> {
  private double value;

  public DummyValueProvider(double initialValue) {
    this.value = initialValue;
  }

  @Override
  public Double get() {
    return this.value;
  }

  @Override
  public void set(Double value) {
    this.value = value;
  }
}
