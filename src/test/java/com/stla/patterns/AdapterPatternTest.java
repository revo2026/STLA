package com.stla.patterns;

import com.stla.patterns.adapter.CsvReportExportAdapter;
import com.stla.patterns.adapter.ReportExportAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Adapter Pattern — Report Export.
 */
@DisplayName("Adapter Pattern: ReportExportAdapter")
class AdapterPatternTest {

    @Test
    @DisplayName("CsvReportExportAdapter should implement interface")
    void shouldImplementInterface() {
        assertInstanceOf(ReportExportAdapter.class, new CsvReportExportAdapter());
    }

    @Test
    @DisplayName("Should return CSV format name")
    void shouldReturnFormatName() {
        CsvReportExportAdapter adapter = new CsvReportExportAdapter();
        assertEquals("CSV", adapter.getFormatName());
    }

    @Test
    @DisplayName("Should export report to file")
    void shouldExportToFile(@TempDir Path tempDir) throws IOException {
        CsvReportExportAdapter adapter = new CsvReportExportAdapter();
        String data = "Name,Email,Role\nJohn,john@test.com,student\nJane,jane@test.com,instructor";
        Path outputPath = tempDir.resolve("report.csv");

        boolean result = adapter.export(data, outputPath.toString());

        assertTrue(result);
        assertTrue(Files.exists(outputPath));
        String content = Files.readString(outputPath);
        assertTrue(content.contains("John"));
        assertTrue(content.contains("instructor"));
    }

    @Test
    @DisplayName("Should return false for invalid path")
    void shouldReturnFalseForInvalidPath() {
        CsvReportExportAdapter adapter = new CsvReportExportAdapter();
        boolean result = adapter.export("data", "/invalid/nonexistent/path/file.csv");
        assertFalse(result);
    }
}
