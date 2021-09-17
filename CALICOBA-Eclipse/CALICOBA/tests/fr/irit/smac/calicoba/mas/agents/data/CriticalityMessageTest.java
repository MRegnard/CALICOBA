package fr.irit.smac.calicoba.mas.agents.data;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import fr.irit.smac.calicoba.mas.agents.ObjectiveAgent;
import fr.irit.smac.calicoba.mas.agents.actions.Direction;
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
    CriticalityMessage m1 = new CriticalityMessage(oa, 1.0, Direction.INCREASE);
    CriticalityMessage m2 = new CriticalityMessage(oa, 1.0, Direction.INCREASE);
    Assertions.assertEquals(m1, m2);
  }

  @Test
  void testNotEqualsCriticality() {
    ObjectiveAgent oa = new ObjectiveAgent("obj", new BaseCriticalityFunction(Collections.singletonList("m")) {
      @Override
      protected double getImpl(Map<String, Double> parameterValues) {
        return 0;
      }
    });
    CriticalityMessage m1 = new CriticalityMessage(oa, 1.0, Direction.INCREASE);
    CriticalityMessage m2 = new CriticalityMessage(oa, 2.0, Direction.INCREASE);
    Assertions.assertNotEquals(m1, m2);
  }

  @Test
  void testNotEqualsVariation() {
    ObjectiveAgent oa = new ObjectiveAgent("obj", new BaseCriticalityFunction(Collections.singletonList("m")) {
      @Override
      protected double getImpl(Map<String, Double> parameterValues) {
        return 0;
      }
    });
    CriticalityMessage m1 = new CriticalityMessage(oa, 1.0, Direction.INCREASE);
    CriticalityMessage m2 = new CriticalityMessage(oa, 1.0, Direction.DECREASE);
    Assertions.assertNotEquals(m1, m2);
  }
}
