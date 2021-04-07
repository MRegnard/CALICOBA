package fr.irit.smac.calicoba.mas.agents;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.data.CriticalityFunctionParameters;
import fr.irit.smac.calicoba.mas.model_attributes.IValueProvider;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import fr.irit.smac.calicoba.test_util.DummyValueProvider;

class Scenario5Test {
  private Calicoba calicoba;
  private ParameterAgent param;

  @BeforeEach
  void setUpBeforeClass() {
    this.calicoba = new Calicoba(false);

    this.calicoba.addParameter(new WritableModelAttribute<>(new DummyValueProvider(0), "param", 0.0, 10.0));
    this.param = (ParameterAgent) this.calicoba.getAgentById("ParameterAgent_1");
    this.calicoba
        .addMeasure(new ReadableModelAttribute<Double, IValueProvider<Double>>(() -> this.param.getAttributeValue(),
            "measure", this.param.getAttributeMinValue(), this.param.getAttributeMaxValue()));
    this.calicoba.addObjective("obj", new CriticalityFunctionParameters(0, 1, 2, 3, 4, 5), "measure");
  }

  @Test
  void testPositiveCorrelation() {
    Map<String, Map<String, Number>> matrix = new HashMap<>();
    matrix.put("measure", Collections.singletonMap("param", 1));
    this.calicoba.setCorrelationMatrix(matrix);
    this.calicoba.setup();
    this.calicoba.step();
    Assertions.assertEquals(ParameterAgent.State.NOMIMAL, this.param.getState());
    Assertions.assertEquals(1, this.param.getLastAction());
  }

  @Test
  void testNegativeCorrelation() {
    Map<String, Map<String, Number>> matrix = new HashMap<>();
    matrix.put("measure", Collections.singletonMap("param", -1));
    this.calicoba.setCorrelationMatrix(matrix);
    this.calicoba.setup();
    this.calicoba.step();
    Assertions.assertEquals(ParameterAgent.State.NOMIMAL, this.param.getState());
    Assertions.assertEquals(-1, this.param.getLastAction());
  }
}
