package com.stla.ui.components;

import javafx.scene.chart.*;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.geometry.Insets;

/**
 * Chart factory for creating professionally styled analytics charts.
 */
public class ChartFactory {

    /** Revenue trend line chart */
    public static VBox createRevenueChart(String title, double[] values, String[] labels) {
        VBox container = new VBox(8);
        container.getStyleClass().add("chart-card");
        container.setPadding(new Insets(20));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("heading-3");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Period");
        yAxis.setLabel("Revenue ($)");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(null);
        chart.setCreateSymbols(true);
        chart.setAnimated(true);
        chart.setLegendVisible(false);
        chart.setPrefHeight(250);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Revenue");
        for (int i = 0; i < values.length && i < labels.length; i++) {
            series.getData().add(new XYChart.Data<>(labels[i], values[i]));
        }
        chart.getData().add(series);
        container.getChildren().addAll(titleLabel, chart);
        return container;
    }

    /** Enrollment bar chart */
    public static VBox createBarChart(String title, String[] categories, double[] values) {
        VBox container = new VBox(8);
        container.getStyleClass().add("chart-card");
        container.setPadding(new Insets(20));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("heading-3");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle(null);
        chart.setAnimated(true);
        chart.setLegendVisible(false);
        chart.setPrefHeight(250);
        chart.setBarGap(4);
        chart.setCategoryGap(20);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 0; i < categories.length && i < values.length; i++) {
            series.getData().add(new XYChart.Data<>(categories[i], values[i]));
        }
        chart.getData().add(series);
        container.getChildren().addAll(titleLabel, chart);
        return container;
    }

    /** Distribution pie chart */
    public static VBox createPieChart(String title, String[] labels, double[] values) {
        VBox container = new VBox(8);
        container.getStyleClass().add("chart-card");
        container.setPadding(new Insets(20));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("heading-3");

        PieChart chart = new PieChart();
        chart.setTitle(null);
        chart.setAnimated(true);
        chart.setLegendVisible(true);
        chart.setPrefHeight(250);

        for (int i = 0; i < labels.length && i < values.length; i++) {
            chart.getData().add(new PieChart.Data(labels[i], values[i]));
        }
        container.getChildren().addAll(titleLabel, chart);
        return container;
    }

    /** Area chart for trends */
    public static VBox createAreaChart(String title, double[] values, String[] labels) {
        VBox container = new VBox(8);
        container.getStyleClass().add("chart-card");
        container.setPadding(new Insets(20));

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("heading-3");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        AreaChart<String, Number> chart = new AreaChart<>(xAxis, yAxis);
        chart.setTitle(null);
        chart.setAnimated(true);
        chart.setLegendVisible(false);
        chart.setPrefHeight(250);
        chart.setCreateSymbols(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 0; i < values.length && i < labels.length; i++) {
            series.getData().add(new XYChart.Data<>(labels[i], values[i]));
        }
        chart.getData().add(series);
        container.getChildren().addAll(titleLabel, chart);
        return container;
    }
}
