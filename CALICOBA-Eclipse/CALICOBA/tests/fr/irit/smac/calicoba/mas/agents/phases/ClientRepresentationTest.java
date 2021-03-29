package fr.irit.smac.calicoba.mas.agents.phases;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.irit.smac.calicoba.mas.agents.data.Direction;
import fr.irit.smac.calicoba.mas.agents.data.VariationRequest;

class ClientRepresentationTest {
  private ClientRepresentation cr;

  @BeforeEach
  void setUp() throws Exception {
    this.cr = new ClientRepresentation();
  }

  @Test
  void testNoPhasesDelay() {
    Assertions.assertEquals(0, this.cr.estimateDelay());
  }

  @Test
  void test1PhaseDelay() {
    this.cr.update(new VariationRequest("agent", 1.0, Direction.INCREASE), 2.0, 0);
    Assertions.assertEquals(0, this.cr.estimateDelay());
  }

  @Test
  void test2PhasesDelay() {
    this.cr.update(new VariationRequest("agent", 1.0, Direction.INCREASE), 2.0, 0);
    this.cr.update(new VariationRequest("agent", 1.0, Direction.DECREASE), 2.0, 1);
    Assertions.assertEquals(0, this.cr.estimateDelay());
  }

  @Test
  void test3PhasesDelay() {
    this.cr.update(new VariationRequest("agent", 1.0, Direction.INCREASE), 2.0, 0);
    this.cr.update(new VariationRequest("agent", 1.0, Direction.DECREASE), 2.0, 1);
    this.cr.update(new VariationRequest("agent", 1.0, Direction.INCREASE), 2.0, 2);
    Assertions.assertEquals(1, this.cr.estimateDelay());
  }

  @Test
  void testFirstCycleNoError() {
    Assertions.assertDoesNotThrow(() -> this.cr.update(new VariationRequest("agent", 1.0, Direction.INCREASE), 1.0, 1));
  }

  @Test
  void testMissingCycleError() {
    this.cr.update(new VariationRequest("agent", 1.0, Direction.INCREASE), 1.0, 1);
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> this.cr.update(new VariationRequest("agent", 1.0, Direction.INCREASE), 1.0, 3));
  }

  @Test
  void testSameCycleError() {
    this.cr.update(new VariationRequest("agent", 1.0, Direction.INCREASE), 1.0, 1);
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> this.cr.update(new VariationRequest("agent", 1.0, Direction.INCREASE), 1.0, 1));
  }
}
