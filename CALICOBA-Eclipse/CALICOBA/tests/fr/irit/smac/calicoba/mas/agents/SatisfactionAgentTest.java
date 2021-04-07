package fr.irit.smac.calicoba.mas.agents;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.irit.smac.calicoba.mas.agents.data.CriticalityFunctionParameters;
import fr.irit.smac.calicoba.mas.model_attributes.IValueProviderSetter;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;
import fr.irit.smac.calicoba.test_util.DummyValueProvider;

class SatisfactionAgentTest {
  private static IValueProviderSetter<Double> provider;
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
    provider = new DummyValueProvider(0);
    params = new CriticalityFunctionParameters(INF, INFL1, NULL_MIN, NULL_MAX, INFL2, SUP);
  }

  @BeforeEach
  void setUp() throws Exception {
    provider.set(0.0);
    this.mAgent = new MeasureAgent(new ReadableModelAttribute<>(provider, "measure", INF, SUP));
    this.agent = new SatisfactionAgent(NAME, this.mAgent, params);
  }

  @Test
  void testPerceive() {
    provider.set(1.0);
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
    provider.set(NULL_MIN);
    this.mAgent.perceive();
    this.agent.perceive();
    this.agent.decideAndAct();
    Assertions.assertEquals(0.0, this.agent.getCriticality());
  }

  @Test
  void testGetMinCriticalityUp() {
    provider.set(NULL_MAX);
    this.mAgent.perceive();
    this.agent.perceive();
    this.agent.decideAndAct();
    Assertions.assertEquals(0.0, this.agent.getCriticality());
  }

  @Test
  void testGetMaxCriticalityDown() {
    provider.set(INF);
    this.mAgent.perceive();
    this.agent.perceive();
    this.agent.decideAndAct();
    Assertions.assertEquals(100.0, this.agent.getCriticality());
  }

  @Test
  void testGetMaxCriticalityUp() {
    provider.set(SUP);
    this.mAgent.perceive();
    this.agent.perceive();
    this.agent.decideAndAct();
    Assertions.assertEquals(100.0, this.agent.getCriticality());
  }
}
