package fr.irit.smac.calicoba.mas.agents;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.criticality.CriticalityFunctionParameters;
import fr.irit.smac.calicoba.mas.agents.criticality.DefaultCriticalityFunction;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import fr.irit.smac.calicoba.test_util.DummyValueProvider;

class Scenario2Test {
  private static Calicoba calicoba;
  private static ParameterAgent param;

  @BeforeAll
  static void setUpBeforeClass() {
    DummyValueProvider p = new DummyValueProvider(0);
    calicoba = new Calicoba(false, null, false, 0);
    calicoba.addParameter(new WritableModelAttribute<>(p, "param", 0.0, 10.0));
    calicoba.addMeasure(new ReadableModelAttribute<>(p, "m", 0.0, 10.0));
    calicoba.addObjective("obj1",
        new DefaultCriticalityFunction("m", new CriticalityFunctionParameters(0, 1, 2, 3, 4, 5)));
    calicoba.addObjective("obj2",
        new DefaultCriticalityFunction("m", new CriticalityFunctionParameters(-1, 1, 2, 3, 4, 5)));
    calicoba.setInfluenceFunction((pName, pValue, objName, objCrit) -> -1.0);
    calicoba.setup();
    param = (ParameterAgent) calicoba.getAgentById("ParameterAgent_1");
  }

  @Test
  void test() {
    calicoba.step();
    Assertions.assertEquals(1, param.getLastAction());
  }
}
