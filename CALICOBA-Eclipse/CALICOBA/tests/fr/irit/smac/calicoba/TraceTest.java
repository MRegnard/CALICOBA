package fr.irit.smac.calicoba;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.ParameterAgent;
import fr.irit.smac.calicoba.mas.agents.data.CriticalityFunctionParameters;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import fr.irit.smac.calicoba.test_util.DummyValueProvider;

public class TraceTest {
  public static void main(String[] args) {
    DummyValueProvider paramProvider = new DummyValueProvider(0);
    DummyValueProvider measureProvider = new DummyValueProvider(0);
    Calicoba calicoba = new Calicoba(true);
    calicoba.addParameter(new WritableModelAttribute<>(paramProvider, "p", -10, 10));
    calicoba.addMeasure(new ReadableModelAttribute<>(measureProvider, "m", -20, 20));
    calicoba.addObjective("obj", new CriticalityFunctionParameters(-15, -10, 4, 5, 10, 15), "m");
    ParameterAgent p = (ParameterAgent) calicoba.getAgentById("ParameterAgent_1");
    Map<String, Map<String, Number>> matrix = new HashMap<>();
    matrix.put("m", Collections.singletonMap("p", 1));
    calicoba.setCorrelationMatrix(matrix);
    calicoba.setup();
    while (true) {
//      measureProvider.step();
      calicoba.step();
      System.out.println(p.getLastAction());
      if (p.getLastAction() != 0) {
        measureProvider.set(paramProvider.get());
      }
    }
  }
}
