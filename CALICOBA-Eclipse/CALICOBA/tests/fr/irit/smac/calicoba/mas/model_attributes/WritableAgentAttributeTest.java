package fr.irit.smac.calicoba.mas.model_attributes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.irit.smac.calicoba.test_util.DummyValueProvider;

class WritableAgentAttributeTest {
  private static WritableModelAttribute<Double, IValueProviderSetter<Double>> attr;
  private static final double VALUE = 3;
  private static final double MIN = 0;
  private static final double MAX = 10;
  private static final String NAME = "attr";

  @BeforeEach
  void setUp() throws Exception {
    attr = new WritableModelAttribute<>(new DummyValueProvider(VALUE), NAME, MIN, MAX);
  }

  @Test
  void testGetName() {
    Assertions.assertEquals(NAME, attr.getName());
  }

  @Test
  void testGetValue() {
    Assertions.assertEquals(VALUE, attr.getValue());
  }

  @Test
  void testGetMin() {
    Assertions.assertEquals(MIN, attr.getMin());
  }

  @Test
  void testGetMax() {
    Assertions.assertEquals(MAX, attr.getMax());
  }

  @Test
  void testSetValue() {
    attr.setValue(4.0);
    Assertions.assertEquals(4.0, attr.getValue());
  }
}
