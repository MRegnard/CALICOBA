model Road

/**
 * This species represents a road.
 * A road has no special behavior.
 */
species Road skills: [skill_road] {
  /** Wether this road is oneway only (OSM attribute). */
  string oneway;
  /** Geometry for display only. Set at creation. */
  geometry geom_display;

  aspect geom {
    draw geom_display border: #gray color: #gray;
  }
}
