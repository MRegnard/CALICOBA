package fr.irit.smac.calicoba.mas.agents.criticality;

/**
 * A normalizer that uses the all-time absolute max of the passed values.
 *
 * @author Damien Vergnet
 */
public class AllTimeAbsoluteNormalizer implements Normalizer {
  private double max = Double.NEGATIVE_INFINITY;

  @Override
  public double normalize(final double value) {
    if (Math.abs(value) > this.max) {
      this.max = Math.abs(value);
    }
    return this.max == 0 ? value : value / this.max;
  }
}
