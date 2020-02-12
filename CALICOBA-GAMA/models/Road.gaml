model Road

species Road skills: [skill_road] {
  string oneway;
  geometry geom_display;

  aspect geom {
    draw geom_display border: #gray color: #gray;
  }
}
