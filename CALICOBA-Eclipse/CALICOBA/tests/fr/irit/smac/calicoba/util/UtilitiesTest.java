package fr.irit.smac.calicoba.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import fr.irit.smac.util.Utilities;

class UtilitiesTest {
  @Test
  void testGetCycleFound2() {
    assertArrayEquals(new Number[] { 2, 3 }, Utilities.getCycle(Arrays.asList(2, 3, 2, 3, 1, 2, 3)).get());
    assertArrayEquals(new Number[] { 2.0, 3.0 },
        Utilities.getCycle(Arrays.asList(2.0, 3.0, 2.0, 3.0, 1.0, 2.0, 3.0)).get());
  }

  @Test
  void testGetCycleFound3() {
    assertArrayEquals(new Number[] { 2, 3, 2 }, Utilities.getCycle(Arrays.asList(2, 3, 2, 2, 3, 2, 2, 3)).get());
    assertArrayEquals(new Number[] { 2.0, 3.0, 2.0 },
        Utilities.getCycle(Arrays.asList(2.0, 3.0, 2.0, 2.0, 3.0, 2.0, 2.0, 3.0)).get());
  }

  @Test
  void testGetCycleNotFound() {
    assertFalse(Utilities.getCycle(Arrays.asList(1, 2, 3, 2, 3, 3, 2, 3)).isPresent());
    assertFalse(Utilities.getCycle(Arrays.asList(1.0, 2.0, 3.0, 2.0, 3.0, 3.0, 2.0, 3.0)).isPresent());
  }

  @Test
  void testGetCycleNotFoundLine() {
    assertFalse(Utilities.getCycle(Arrays.asList(1, 1, 1, 1, 1, 1, 1)).isPresent());
    assertFalse(Utilities.getCycle(Arrays.asList(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)).isPresent());
  }
}
