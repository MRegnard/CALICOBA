package fr.irit.smac.calicoba.mas.agents.phases;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.irit.smac.calicoba.mas.agents.actions.Action;
import fr.irit.smac.calicoba.mas.agents.actions.Direction;

class ActionTest {
  private static final String NAME = "sender";
  private static final Direction DIR = Direction.INCREASE;
  private static final int DELAY = 1;

  private Action action;

  @BeforeEach
  void setUp() throws Exception {
    this.action = new Action(NAME, DIR, DELAY);
  }

  @Test
  void testGetBeneficiaryName() {
    Assertions.assertEquals(NAME, this.action.getBeneficiaryName());
  }

  @Test
  void testGetDirection() {
    Assertions.assertEquals(DIR, this.action.getDirection());
  }

  @Test
  void testIsExecuted() {
    this.action.setExecuted();
    Assertions.assertTrue(this.action.isExecuted());
  }

  @Test
  void testIsNotExecuted() {
    Assertions.assertFalse(this.action.isExecuted());
  }

  @Test
  void testIsDelayOver() {
    Action action = new Action(NAME, DIR, 0);
    action.setExecuted();
    Assertions.assertTrue(action.isDelayOver());
  }

  @Test
  void testIsDelayNotOverExecuted() {
    this.action.setExecuted();
    Assertions.assertFalse(this.action.isDelayOver());
  }

  @Test
  void testIsDelayNotOverNotExecuted() {
    Assertions.assertFalse(this.action.isDelayOver());
  }

  @Test
  void testIsDelayNotOverZeroDelayNotExecuted() {
    Action action = new Action(NAME, DIR, 0);
    Assertions.assertFalse(action.isDelayOver());
  }

  @Test
  void testDecreaseRemainingSteps() {
    this.action.setExecuted();
    this.action.decreaseRemainingSteps();
    Assertions.assertTrue(this.action.isDelayOver());
  }
}
