package fr.irit.smac.calicoba.mas.agents.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class VariationRequestTest {
  @Test
  void testGetAndSetLastAgent() {
    VariationRequest r = new VariationRequest("agent", 0.0, Direction.STAY);
    Assertions.assertNull(r.getLastAgent());
    r.setLastAgent("last");
    Assertions.assertEquals("last", r.getLastAgent());
  }

  @Test
  void testGetOppositeRequestIncrease() {
    Assertions.assertEquals(new VariationRequest("agent", 1.0, Direction.DECREASE),
        new VariationRequest("agent", 1.0, Direction.INCREASE).getOppositeRequest());
  }

  @Test
  void testGetOppositeRequestDecrease() {
    Assertions.assertEquals(new VariationRequest("agent", 1.0, Direction.INCREASE),
        new VariationRequest("agent", 1.0, Direction.DECREASE).getOppositeRequest());
  }

  @Test
  void testGetOppositeRequestStay() {
    Assertions.assertEquals(new VariationRequest("agent", 1.0, Direction.STAY),
        new VariationRequest("agent", 1.0, Direction.STAY));
  }

  @Test
  void testEqualsNoLastAgent() {
    VariationRequest r1 = new VariationRequest("agent", 1.0, Direction.STAY);
    VariationRequest r2 = new VariationRequest("agent", 1.0, Direction.STAY);
    Assertions.assertEquals(r1, r2);
  }

  @Test
  void testEqualsSameLastAgent() {
    VariationRequest r1 = new VariationRequest("agent", 1.0, Direction.STAY);
    r1.setLastAgent("last");
    VariationRequest r2 = new VariationRequest("agent", 1.0, Direction.STAY);
    r2.setLastAgent("last");
    Assertions.assertEquals(r1, r2);
  }

  @Test
  void testEqualsDifferentLastAgent() {
    VariationRequest r1 = new VariationRequest("agent", 1.0, Direction.STAY);
    r1.setLastAgent("last");
    VariationRequest r2 = new VariationRequest("agent", 1.0, Direction.STAY);
    r2.setLastAgent("tsal");
    Assertions.assertEquals(r1, r2);
  }
}
