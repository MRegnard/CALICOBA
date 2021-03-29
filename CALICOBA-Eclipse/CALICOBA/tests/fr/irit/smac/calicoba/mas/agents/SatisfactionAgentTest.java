package fr.irit.smac.calicoba.mas.agents;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.irit.smac.calicoba.mas.agents.data.CriticalityFunctionParameters;
import fr.irit.smac.calicoba.mas.model_attributes.IValueProvider;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;

class SatisfactionAgentTest {
  private static DummyProvider provider;
  private static final String NAME = "obj";
  private static final double INF = 0;
  private static final double INFL1 = 1;
  private static final double NULL_MIN = 2;
  private static final double NULL_MAX = 3;
  private static final double INFL2 = 4;
  private static final double SUP = 5;
  private static CriticalityFunctionParameters params;

  private SatisfactionAgent agent;
  private MeasureAgent mAgent;

  @BeforeAll
  static void setUpBeforeClass() throws Exception {
    provider = new DummyProvider();
    params = new CriticalityFunctionParameters(INF, INFL1, NULL_MIN, NULL_MAX, INFL2, SUP);
  }

  @BeforeEach
  void setUp() throws Exception {
    provider.value = 0.0;
    this.mAgent = new MeasureAgent(new ReadableModelAttribute<>(provider, "measure", INF, SUP));
    this.agent = new SatisfactionAgent(NAME, this.mAgent, params);
  }

  @Test
  void testPerceive() {
    provider.value = 1.0;
    this.mAgent.perceive();
    this.agent.perceive();
    Assertions.assertEquals(1.0, this.agent.getRelativeValue());
  }

  @Test
  void testGetName() {
    Assertions.assertEquals(NAME, this.agent.getName());
  }

  @Test
  void testGetInitCriticality() {
    Assertions.assertEquals(0.0, this.agent.getCriticality());
  }

  @Test
  void testGetMinCriticalityDown() {
    provider.value = NULL_MIN;
    this.mAgent.perceive();
    this.agent.perceive();
    this.agent.decideAndAct();
    Assertions.assertEquals(0.0, this.agent.getCriticality());
  }

  @Test
  void testGetMinCriticalityUp() {
    provider.value = NULL_MAX;
    this.mAgent.perceive();
    this.agent.perceive();
    this.agent.decideAndAct();
    Assertions.assertEquals(0.0, this.agent.getCriticality());
  }

  @Test
  void testGetMaxCriticalityDown() {
    provider.value = INF;
    this.mAgent.perceive();
    this.agent.perceive();
    this.agent.decideAndAct();
    Assertions.assertEquals(100.0, this.agent.getCriticality());
  }

  @Test
  void testGetMaxCriticalityUp() {
    provider.value = SUP;
    this.mAgent.perceive();
    this.agent.perceive();
    this.agent.decideAndAct();
    Assertions.assertEquals(100.0, this.agent.getCriticality());
  }

  static class DummyProvider implements IValueProvider<Double> {
    public double value;

    @Override
    public Double getValue() {
      return this.value;
    }
  }
}
