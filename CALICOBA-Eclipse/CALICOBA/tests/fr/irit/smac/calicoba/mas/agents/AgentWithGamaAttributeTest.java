package fr.irit.smac.calicoba.mas.agents;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.irit.smac.calicoba.mas.agents.actions.Direction;
import fr.irit.smac.calicoba.mas.agents.criticality.CriticalityFunction;
import fr.irit.smac.calicoba.mas.agents.messages.CriticalityMessage;
import fr.irit.smac.calicoba.mas.model_attributes.IValueProvider;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;

class AgentWithGamaAttributeTest {
  private static DummyProvider provider;
  private static final double VALUE = 3;
  private static final double MIN = 0;
  private static final double MAX = 10;
  private static final String NAME = "attr";

  @BeforeAll
  static void setUpBeforeClass() throws Exception {
    provider = new DummyProvider();
  }

  private AgentWithGamaAttribute<ReadableModelAttribute<Double, IValueProvider<Double>>, IValueProvider<Double>> agent;
  private ReadableModelAttribute<Double, IValueProvider<Double>> attribute;

  @BeforeEach
  void setUp() throws Exception {
    provider.value = VALUE;
    this.attribute = new ReadableModelAttribute<>(provider, NAME, MIN, MAX);
    this.agent = new AgentWithGamaAttribute<ReadableModelAttribute<Double, IValueProvider<Double>>, IValueProvider<Double>>(
        this.attribute) {
    };
  }

  @Test
  void testPerceive() {
    provider.value = 0.0;
    this.agent.perceive();
    Assertions.assertEquals(0.0, this.agent.getAttributeValue());
  }

  @Test
  void testOnRequest() {
    CriticalityMessage r = new CriticalityMessage(new ObjectiveAgent("obj", new CriticalityFunction() {
      @Override
      public List<String> getParameterNames() {
        return Collections.emptyList();
      }

      @Override
      public double get(Map<String, Double> parameterValues) {
        return 0;
      }
    }), 1.0, Direction.INCREASE);
    this.agent.onMessage(r);
    Assertions.assertEquals(1, this.agent.messages.size());
    Assertions.assertEquals(r, this.agent.messages.stream().findFirst().get());
  }

  @Test
  void testGetAttribute() {
    Assertions.assertTrue(this.attribute == this.agent.getAttribute());
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

  static class DummyProvider implements IValueProvider<Double> {
    public Double value;

    @Override
    public Double get() {
      return this.value;
    }
  }
}
