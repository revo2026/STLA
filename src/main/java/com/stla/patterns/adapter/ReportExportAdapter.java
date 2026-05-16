package com.stla.patterns.adapter;

/**
 * Adapter Pattern: Report export adapter for different formats.
 */
public interface ReportExportAdapter {
    boolean export(String reportData, String outputPath);
    String getFormatName();
}
