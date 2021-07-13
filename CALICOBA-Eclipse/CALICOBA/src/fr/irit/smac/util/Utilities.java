package fr.irit.smac.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public class Utilities {
  /**
   * Looks for a the smallest cycle (length > 2) in the given ordered collection.
   * 
   * @param values Number sequence to check.
   * @return The sequence as a Number array if found; an empty Optional otherwise.
   */
  public static Optional<Number[]> getCycle(Collection<? extends Number> values) {
    Number[] a = values.toArray(new Number[values.size()]);

    for (int length = 2; length <= a.length / 2; length++) {
      Number[] a1 = Arrays.copyOfRange(a, 0, length);
      Number[] a2 = Arrays.copyOfRange(a, length, 2 * length);
      // Both arrays are equal AND not all elements are the same in the first array
      if (Arrays.equals(a1, a2) && Arrays.stream(a1).skip(1).anyMatch(e -> !e.equals(a1[0]))) {
        return Optional.of(a1);
      }
    }

    return Optional.empty();
  }

  /**
   * This function checks whether there is a cycle that begins at the start of the
   * specified ordered collection.
   * 
   * @param values Number sequence to check.
   * @return True if a cycle was found; false otherwise.
   */
  public static boolean hasCycle(Collection<? extends Number> values) {
    return getCycle(values).isPresent();
  }

  private Utilities() {
  }
}
