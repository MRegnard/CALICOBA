package fr.irit.smac.calicoba.mas.model_attributes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ReadableModelAttributeTest {
  private static ReadableModelAttribute<Double, IValueProvider<Double>> attr;
  private static final double VALUE = 3;
  private static final double MIN = 0;
  private static final double MAX = 0;
  private static final String NAME = "attr";

  @BeforeAll
  static void setUpBeforeClass() throws Exception {
    attr = new ReadableModelAttribute<>(() -> VALUE, NAME, MIN, MAX);
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
}
