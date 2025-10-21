package evaluation;

public enum Metric {
  PRECISION,
  RECALL;

  public static Metric buildMetric(String metric) {
    switch (metric.toLowerCase()) {
      case "precision":
        return PRECISION;
      case "recall":
        return RECALL;
      default:
        throw new IllegalArgumentException("Unsupported metric: " + metric);
    }
  }
}
