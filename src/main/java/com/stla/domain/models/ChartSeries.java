package com.stla.domain.models;

/**
 * Labels and numeric values for dashboard charts.
 */
public class ChartSeries {
    private final String[] labels;
    private final double[] values;

    public ChartSeries(String[] labels, double[] values) {
        this.labels = labels != null ? labels : new String[0];
        this.values = values != null ? values : new double[0];
    }

    public String[] getLabels() { return labels; }
    public double[] getValues() { return values; }

    public boolean isEmpty() {
        return labels.length == 0;
    }
}
