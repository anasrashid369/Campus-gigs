package com.campusgigs.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Writes tabular data as CSV (RFC 4180): comma separated, CRLF line endings,
 * header row first, and fields wrapped in double quotes only when they contain
 * a comma, a quote or a line break. Quotes inside a field are doubled.
 * <p>
 * Used by the ReportService to export admin reports. Values are passed as
 * strings so the caller decides how numbers and dates are formatted;
 * {@code null} values are written as empty fields.
 */
public final class CsvExporter {

    /** Byte order mark, so Excel reads the file as UTF-8 instead of the system code page. */
    static final String UTF8_BOM = "﻿";

    private static final char SEPARATOR = ',';
    private static final String LINE_END = "\r\n";

    private CsvExporter() {
    }

    /**
     * Writes the CSV to a UTF-8 file with a BOM, replacing the file if it exists.
     * The data is checked before the file is opened, so invalid data never
     * overwrites an existing export.
     *
     * @throws IllegalArgumentException if there are no headers or a row does not have one value per header
     * @throws IOException              if the file cannot be written
     */
    public static void write(Path file, List<String> headers, List<List<String>> rows) throws IOException {
        Objects.requireNonNull(file, "file");
        String csv = toCsv(headers, rows);
        Files.writeString(file, UTF8_BOM + csv, StandardCharsets.UTF_8);
    }

    /**
     * Builds the CSV text (without a BOM), header row first.
     *
     * @throws IllegalArgumentException if there are no headers or a row does not have one value per header
     */
    public static String toCsv(List<String> headers, List<List<String>> rows) {
        validate(headers, rows);
        StringBuilder csv = new StringBuilder();
        appendLine(csv, headers);
        for (List<String> row : rows) {
            appendLine(csv, row);
        }
        return csv.toString();
    }

    /** Escapes a single value so it can be placed in a CSV field. */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuotes = value.indexOf(SEPARATOR) >= 0 || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        if (!needsQuotes) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static void appendLine(StringBuilder csv, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                csv.append(SEPARATOR);
            }
            csv.append(escape(values.get(i)));
        }
        csv.append(LINE_END);
    }

    private static void validate(List<String> headers, List<List<String>> rows) {
        Objects.requireNonNull(headers, "headers");
        Objects.requireNonNull(rows, "rows");
        if (headers.isEmpty()) {
            throw new IllegalArgumentException("A CSV export needs at least one column header");
        }
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = Objects.requireNonNull(rows.get(i), "row " + (i + 1));
            if (row.size() != headers.size()) {
                throw new IllegalArgumentException("Row " + (i + 1) + " has " + row.size()
                        + " values but there are " + headers.size() + " columns");
            }
        }
    }
}
