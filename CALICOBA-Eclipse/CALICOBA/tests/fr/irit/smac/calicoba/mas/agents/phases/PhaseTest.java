package fr.irit.smac.calicoba.mas.agents.phases;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.irit.smac.calicoba.mas.agents.data.Direction;

class PhaseTest {
  private static final Direction DIR = Direction.INCREASE;
  private static final double CRIT = 1.0;
  private static final double VALUE = 2.0;
  private static final int STEP = 0;

  private Phase phase;

  @BeforeEach
  void setUp() throws Exception {
    this.phase = new Phase(DIR);
    this.phase.update(CRIT, VALUE, STEP);
  }

  @Test
  void testGetDirection() {
    Assertions.assertEquals(DIR, this.phase.getDirection());
  }

  @Test
  void testUpdateMostExtremeValuePositive() {
    int step = STEP + 1;
    this.phase.update(CRIT, VALUE + 1, step);
    Assertions.assertEquals(step, this.phase.getStepForMostExtremeValue());
  }

  @Test
  void testUpdateMostExtremeCriticalityPositive() {
    int step = STEP + 1;
    this.phase.update(CRIT + 1, VALUE, step);
    Assertions.assertEquals(step, this.phase.getStepForMostExtremeCriticality());
  }

  @Test
  void testUpdateMostExtremeValueNegative() {
    int step = STEP + 1;
    this.phase.update(CRIT, -VALUE - 1, step);
    Assertions.assertEquals(step, this.phase.getStepForMostExtremeValue());
  }

  @Test
  void testUpdateMostExtremeCriticalityNegative() {
    int step = STEP + 1;
    this.phase.update(-CRIT - 1, VALUE, step);
    Assertions.assertEquals(step, this.phase.getStepForMostExtremeCriticality());
  }

  @Test
  void testUpdateMostExtremeValueNoChange() {
    this.phase.update(CRIT, VALUE / 2, STEP + 1);
    Assertions.assertEquals(STEP, this.phase.getStepForMostExtremeValue());
  }

  @Test
  void testUpdateMostExtremeCriticalityNoChange() {
    this.phase.update(CRIT / 2, VALUE, STEP + 1);
    Assertions.assertEquals(STEP, this.phase.getStepForMostExtremeCriticality());
  }

  @Test
  void testGetStepForMostExtremeValue() {
    Assertions.assertEquals(STEP, this.phase.getStepForMostExtremeValue());
  }

  @Test
  void testGetStepForMostExtremeCriticality() {
    Assertions.assertEquals(STEP, this.phase.getStepForMostExtremeCriticality());
  }

  @Test
  void testMissingFirstCycleNoError() {
    Assertions.assertDoesNotThrow(() -> this.phase.update(1.0, 1.0, 1));
  }

  @Test
  void testMissingCycleError() {
    this.phase.update(1.0, 1.0, 1);
    Assertions.assertThrows(IllegalArgumentException.class, () -> this.phase.update(1.0, 1.0, 3));
  }

  @Test
  void testSameCycleError() {
    this.phase.update(1.0, 1.0, 1);
    Assertions.assertThrows(IllegalArgumentException.class, () -> this.phase.update(1.0, 1.0, 1));
  }
}
