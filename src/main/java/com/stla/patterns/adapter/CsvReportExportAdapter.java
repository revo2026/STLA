package com.stla.patterns.adapter;

import java.io.FileWriter;
import java.io.IOException;

/**
 * CSV report export adapter.
 */
public class CsvReportExportAdapter implements ReportExportAdapter {

    @Override
    public boolean export(String reportData, String outputPath) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(reportData);
            System.out.println("[CsvExport] Report exported to " + outputPath);
            return true;
        } catch (IOException e) {
            System.err.println("Export error: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getFormatName() { return "CSV"; }
}
