package fr.irit.smac.calicoba.mas.agents;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.criticality.CriticalityFunctionParameters;
import fr.irit.smac.calicoba.mas.agents.criticality.DefaultCriticalityFunction;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import fr.irit.smac.calicoba.test_util.DummyValueProvider;

class Scenario5Test {
  private Calicoba calicoba;
  private ParameterAgent param;

  @Test
  void testNegativeInfluence() {
    this.calicoba = new Calicoba(false, null, false, 0);

    this.calicoba.addParameter(new WritableModelAttribute<>(new DummyValueProvider(0), "param", 0.0, 10.0));
    this.param = (ParameterAgent) this.calicoba.getAgentById("ParameterAgent_1");
    this.calicoba.addMeasure(new ReadableModelAttribute<>(this.param::getAttributeValue, "measure",
        this.param.getAttributeMinValue(), this.param.getAttributeMaxValue()));
    this.calicoba.addObjective("obj",
        new DefaultCriticalityFunction("measure", new CriticalityFunctionParameters(0, 1, 2, 3, 4, 5)));
    this.calicoba.setInfluenceFunction((pName, pValue, objName, objCrit) -> -1.0);
    this.calicoba.setup();
    this.calicoba.step();
    Assertions.assertEquals(1, this.param.getLastAction());
  }

  @Test
  void testPositiveInfluence() {
    this.calicoba = new Calicoba(false, null, false, 0);

    this.calicoba.addParameter(new WritableModelAttribute<>(new DummyValueProvider(0), "param", 0.0, 10.0));
    this.param = (ParameterAgent) this.calicoba.getAgentById("ParameterAgent_1");
    this.calicoba.addMeasure(new ReadableModelAttribute<>(() -> -this.param.getAttributeValue(), "measure",
        this.param.getAttributeMinValue(), this.param.getAttributeMaxValue()));
    this.calicoba.addObjective("obj",
        new DefaultCriticalityFunction("measure", new CriticalityFunctionParameters(0, 1, 2, 3, 4, 5)));
    this.calicoba.setInfluenceFunction((pName, pValue, objName, objCrit) -> 1.0);
    this.calicoba.setup();
    this.calicoba.step();
    Assertions.assertEquals(-1, this.param.getLastAction());
  }
}
