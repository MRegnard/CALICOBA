package fr.irit.smac.calicoba.mas.agents.phases;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
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

//  @Test
//  void test1PhaseDelay() {
//    Iterable<CriticalityMessage> requests = Collections
//        .singleton(new CriticalityMessage(NAME, 1.0, Direction.INCREASE));
//    this.r.update(requests, 2.0, 0);
//    Assertions.assertEquals(0, this.r.estimateDelay(NAME));
//  }
//
//  @Test
//  void test2PhasesDelay() {
//    Iterable<CriticalityMessage> requests = Collections
//        .singleton(new CriticalityMessage(NAME, 1.0, Direction.INCREASE));
//    this.r.update(requests, 2.0, 0);
//    requests = Collections.singleton(new CriticalityMessage("agent", 1.0, Direction.DECREASE));
//    this.r.update(requests, 2.0, 1);
//    Assertions.assertEquals(0, this.r.estimateDelay(NAME));
//  }
//
//  @Test
//  void test3PhasesDelay() {
//    Iterable<CriticalityMessage> requests = Collections
//        .singleton(new CriticalityMessage(NAME, 1.0, Direction.INCREASE));
//    this.r.update(requests, 2.0, 0);
//    requests = Collections.singleton(new CriticalityMessage(NAME, 1.0, Direction.DECREASE));
//    this.r.update(requests, 2.0, 1);
//    requests = Collections.singleton(new CriticalityMessage(NAME, 1.0, Direction.INCREASE));
//    this.r.update(requests, 2.0, 2);
//    Assertions.assertEquals(1, this.r.estimateDelay(NAME));
//  }
//
//  @Test
//  void testFirstCycleNoError() {
//    Iterable<CriticalityMessage> requests = Collections
//        .singleton(new CriticalityMessage(NAME, 1.0, Direction.INCREASE));
//    Assertions.assertDoesNotThrow(() -> this.r.update(requests, 1.0, 1));
//  }
//
//  @Test
//  void testMissingCycleError() {
//    Iterable<CriticalityMessage> requests = Collections
//        .singleton(new CriticalityMessage(NAME, 1.0, Direction.INCREASE));
//    this.r.update(requests, 1.0, 1);
//    Assertions.assertThrows(IllegalArgumentException.class,
//        () -> this.r.update(Collections.singleton(new CriticalityMessage(NAME, 1.0, Direction.INCREASE)), 1.0, 3));
//  }
//
//  @Test
//  void testSameCycleError() {
//    Iterable<CriticalityMessage> requests = Collections
//        .singleton(new CriticalityMessage(NAME, 1.0, Direction.INCREASE));
//    this.r.update(requests, 1.0, 1);
//    Assertions.assertThrows(IllegalArgumentException.class,
//        () -> this.r.update(Collections.singleton(new CriticalityMessage(NAME, 1.0, Direction.INCREASE)), 1.0, 1));
//  }
}
