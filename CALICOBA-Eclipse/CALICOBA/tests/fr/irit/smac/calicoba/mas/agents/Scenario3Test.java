package fr.irit.smac.calicoba.mas.agents;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.data.CriticalityFunctionParameters;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import fr.irit.smac.calicoba.test_util.DummyValueProvider;

class Scenario3Test {
  private static Calicoba calicoba;
  private static ParameterAgent param;

  @BeforeAll
  static void setUpBeforeClass() {
    calicoba = new Calicoba(false);
    calicoba.addParameter(new WritableModelAttribute<>(new DummyValueProvider(0), "param", 0.0, 10.0));
    calicoba.addObjective("obj1", new CriticalityFunctionParameters(0, 1, 2, 3, 4, 5), "param");
    calicoba.addObjective("obj2", new CriticalityFunctionParameters(-5, -4, -3, -2, -1, 0), "param");
    calicoba.setup();
    param = (ParameterAgent) calicoba.getAgentById("ParameterAgent_1");
  }

  @Test
  void test() {
    calicoba.step();
    Assertions.assertEquals(ParameterAgent.State.SAME_CRIT_DIFF_DIRECTIONS, param.getState());
  }
}
