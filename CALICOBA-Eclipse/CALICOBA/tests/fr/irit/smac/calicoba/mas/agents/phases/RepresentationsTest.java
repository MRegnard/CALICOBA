package fr.irit.smac.calicoba.mas.agents.phases;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.irit.smac.calicoba.mas.agents.data.Direction;
import fr.irit.smac.calicoba.mas.agents.data.VariationRequest;

class RepresentationsTest {
  private static final String NAME = "agent";

  private Representations r;

  @BeforeEach
  void setUp() throws Exception {
    this.r = new Representations();
  }

  @Test
  void testNoPhasesDelayThrowsError() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> this.r.estimateDelay(NAME));
  }

  @Test
  void test1PhaseDelay() {
    Iterable<VariationRequest> requests = Collections.singleton(new VariationRequest(NAME, 1.0, Direction.INCREASE));
    this.r.update(requests, 2.0, 0);
    Assertions.assertEquals(0, this.r.estimateDelay(NAME));
  }

  @Test
  void test2PhasesDelay() {
    Iterable<VariationRequest> requests = Collections.singleton(new VariationRequest(NAME, 1.0, Direction.INCREASE));
    this.r.update(requests, 2.0, 0);
    requests = Collections.singleton(new VariationRequest("agent", 1.0, Direction.DECREASE));
    this.r.update(requests, 2.0, 1);
    Assertions.assertEquals(0, this.r.estimateDelay(NAME));
  }

  @Test
  void test3PhasesDelay() {
    Iterable<VariationRequest> requests = Collections.singleton(new VariationRequest(NAME, 1.0, Direction.INCREASE));
    this.r.update(requests, 2.0, 0);
    requests = Collections.singleton(new VariationRequest(NAME, 1.0, Direction.DECREASE));
    this.r.update(requests, 2.0, 1);
    requests = Collections.singleton(new VariationRequest(NAME, 1.0, Direction.INCREASE));
    this.r.update(requests, 2.0, 2);
    Assertions.assertEquals(1, this.r.estimateDelay(NAME));
  }

  @Test
  void testFirstCycleNoError() {
    Iterable<VariationRequest> requests = Collections.singleton(new VariationRequest(NAME, 1.0, Direction.INCREASE));
    Assertions.assertDoesNotThrow(() -> this.r.update(requests, 1.0, 1));
  }

  @Test
  void testMissingCycleError() {
    Iterable<VariationRequest> requests = Collections.singleton(new VariationRequest(NAME, 1.0, Direction.INCREASE));
    this.r.update(requests, 1.0, 1);
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> this.r.update(Collections.singleton(new VariationRequest(NAME, 1.0, Direction.INCREASE)), 1.0, 3));
  }

  @Test
  void testSameCycleError() {
    Iterable<VariationRequest> requests = Collections.singleton(new VariationRequest(NAME, 1.0, Direction.INCREASE));
    this.r.update(requests, 1.0, 1);
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> this.r.update(Collections.singleton(new VariationRequest(NAME, 1.0, Direction.INCREASE)), 1.0, 1));
  }
}
