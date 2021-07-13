package fr.irit.smac.calicoba.mas.agents.data;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import fr.irit.smac.calicoba.mas.agents.ObjectiveAgent;
import fr.irit.smac.calicoba.mas.agents.criticality.BaseCriticalityFunction;
import fr.irit.smac.calicoba.mas.agents.messages.CriticalityMessage;

class CriticalityMessageTest {
  @Test
  void testEquals() {
    ObjectiveAgent oa = new ObjectiveAgent("obj", new BaseCriticalityFunction(Collections.singletonList("m")) {
      @Override
      protected double getImpl(Map<String, Double> parameterValues) {
        return 0;
      }
    });
    CriticalityMessage r1 = new CriticalityMessage(oa, 1.0);
    CriticalityMessage r2 = new CriticalityMessage(oa, 1.0);
    Assertions.assertEquals(r1, r2);
  }
}
