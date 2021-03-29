package fr.irit.smac.calicoba.mas.agents;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.irit.smac.calicoba.mas.model_attributes.IValueProvider;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;

class MeasureAgentTest {
  private static ReadableModelAttribute<Double, IValueProvider<Double>> attr;
  private static final double VALUE = 3;
  private static final double MIN = 0;
  private static final double MAX = 0;
  private static final String NAME = "attr";

  @BeforeAll
  public static void setUpBeforeClass() throws Exception {
    attr = new ReadableModelAttribute<>(() -> VALUE, NAME, MIN, MAX);
  }

  private MeasureAgent agent;

  @BeforeEach
  void setUp() throws Exception {
    this.agent = new MeasureAgent(attr);
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
