package com.campusgigs.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvExporterTest {

    private static final List<String> HEADERS = List.of("Type", "Total");

    @Test
    void writesHeaderAndRowsWithCrlf() {
        String csv = CsvExporter.toCsv(HEADERS, List.of(
                List.of("TOPUP", "1500"),
                List.of("ESCROW_HOLD", "400")));

        assertEquals("Type,Total\r\nTOPUP,1500\r\nESCROW_HOLD,400\r\n", csv);
    }

    @Test
    void writesOnlyHeaderWhenThereAreNoRows() {
        assertEquals("Type,Total\r\n", CsvExporter.toCsv(HEADERS, List.of()));
    }

    @Test
    void quotesValuesContainingCommas() {
        assertEquals("\"Logo, banner and poster\"", CsvExporter.escape("Logo, banner and poster"));
    }

    @Test
    void doublesQuotesInsideValues() {
        assertEquals("\"Make a \"\"cool\"\" logo\"", CsvExporter.escape("Make a \"cool\" logo"));
    }

    @Test
    void quotesValuesContainingLineBreaks() {
        assertEquals("\"line one\nline two\"", CsvExporter.escape("line one\nline two"));
        assertEquals("\"a\rb\"", CsvExporter.escape("a\rb"));
    }

    @Test
    void leavesPlainValuesUntouched() {
        assertEquals("Web Development", CsvExporter.escape("Web Development"));
    }

    @Test
    void writesNullAsEmptyField() {
        String csv = CsvExporter.toCsv(HEADERS, List.of(Arrays.asList("REFUND", null)));

        assertEquals("Type,Total\r\nREFUND,\r\n", csv);
    }

    @Test
    void rejectsRowWithWrongNumberOfValues() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> CsvExporter.toCsv(HEADERS, List.of(List.of("only one"))));

        assertEquals("Row 1 has 1 values but there are 2 columns", error.getMessage());
    }

    @Test
    void rejectsEmptyHeaders() {
        assertThrows(IllegalArgumentException.class, () -> CsvExporter.toCsv(List.of(), List.of()));
    }

    @Test
    void writesUtf8FileStartingWithBom(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("report.csv");

        CsvExporter.write(file, List.of("Name", "Skill"), List.of(List.of("Anas", "Café design")));

        byte[] bytes = Files.readAllBytes(file);
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(CsvExporter.UTF8_BOM + "Name,Skill\r\nAnas,Café design\r\n", content);
    }

    @Test
    void replacesExistingFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("report.csv");
        Files.writeString(file, "old content that is longer than the new one");

        CsvExporter.write(file, List.of("Type"), List.of(List.of("TOPUP")));

        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(CsvExporter.UTF8_BOM + "Type\r\nTOPUP\r\n", content);
    }

    @Test
    void invalidDataDoesNotCreateFile(@TempDir Path dir) {
        Path file = dir.resolve("bad.csv");

        assertThrows(IllegalArgumentException.class,
                () -> CsvExporter.write(file, HEADERS, List.of(List.of("a", "b", "c"))));
        assertFalse(Files.exists(file));
    }
}
