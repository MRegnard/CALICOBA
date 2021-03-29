package fr.irit.smac.calicoba.mas.agents.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DirectionTest {
  @Test
  void testGetActionIncrease() {
    Assertions.assertEquals(1, Direction.INCREASE.getAction());
  }

  @Test
  void testGetActionDecrease() {
    Assertions.assertEquals(-1, Direction.DECREASE.getAction());
  }

  @Test
  void testGetActionStay() {
    Assertions.assertEquals(0, Direction.STAY.getAction());
  }

  @Test
  void testGetOppositeIncrease() {
    Assertions.assertEquals(Direction.DECREASE, Direction.INCREASE.getOpposite());
  }

  @Test
  void testGetOppositeDecrease() {
    Assertions.assertEquals(Direction.INCREASE, Direction.DECREASE.getOpposite());
  }

  @Test
  void testGetOppositeStay() {
    Assertions.assertEquals(Direction.STAY, Direction.STAY.getOpposite());
  }
}
