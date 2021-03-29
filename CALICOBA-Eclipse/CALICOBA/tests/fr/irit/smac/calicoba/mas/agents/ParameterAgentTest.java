package fr.irit.smac.calicoba.mas.agents;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.irit.smac.calicoba.mas.model_attributes.IValueProviderSetter;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;

class ParameterAgentTest {
  private static WritableModelAttribute<Double, IValueProviderSetter<Double>> attr;
  private static final double VALUE = 3;
  private static final double MIN = 0;
  private static final double MAX = 10;
  private static final String NAME = "attr";

  @BeforeAll
  static void setUpBeforeClass() throws Exception {
    attr = new WritableModelAttribute<>(new IValueProviderSetter<Double>() {
      private Double value = VALUE;

      @Override
      public Double getValue() {
        return this.value;
      }

      @Override
      public void setValue(Double value) {
        this.value = value;
      }
    }, NAME, MIN, MAX);
  }

  private ParameterAgent agent;

  @BeforeEach
  void setUp() throws Exception {
    this.agent = new ParameterAgent(attr);
  }

  @Test
  void testGetAttributeName() {
    Assertions.assertEquals(NAME, this.agent.getAttributeName());
  }

  @Test
  void testGetAttributeValue() {
    this.agent.perceive();
    Assertions.assertEquals(VALUE, this.agent.getAttributeValue());
  }

  @Test
  void testGetAttributeMinValue() {
    Assertions.assertEquals(MIN, this.agent.getAttributeMinValue());
  }

  @Test
  void testGetAttributeMaxValue() {
    Assertions.assertEquals(MAX, this.agent.getAttributeMaxValue());
  }
}
