package patmal.workshops.student.impl.logic;

import org.apache.commons.csv.CSVException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.Student;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Registers students from a UTF-8 CSV file, with Apache Commons CSV.
 * <p>
 * The first line must be the header {@code id,name,email}; every other line is one student.
 * Empty lines are ignored and values may be quoted. Each line is registered through
 * {@link WorkshopSystem#createStudent}, so exactly the same rules apply as for a direct call.
 * The lines are processed in file order and the import stops at the first rejected line:
 * the students of the lines before it stay registered.
 */
final class StudentCsvImporter {

    private static final Logger LOGGER = LogManager.getLogger(StudentCsvImporter.class);

    private static final List<String> HEADER = List.of("id", "name", "email");

    /** Some editors (Excel, for example) start a UTF-8 file with a byte order mark. */
    private static final String BYTE_ORDER_MARK = "﻿";

    /** Comma separated, values optionally quoted with ", empty lines ignored (RFC 4180). */
    private static final CSVFormat FORMAT = CSVFormat.DEFAULT;

    private final WorkshopSystem system;

    StudentCsvImporter(WorkshopSystem system) {
        this.system = system;
    }

    /**
     * Registers the students of the file.
     *
     * @return the students registered by this import, in file order
     * @throws IllegalArgumentException if the header is missing or wrong, or at the first rejected line
     * @throws UncheckedIOException     if the file cannot be read
     */
    List<Student> importStudents(File source) {
        List<Student> imported = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(source, StandardCharsets.UTF_8, FORMAT)) {
            Iterator<CSVRecord> lines = parser.iterator();
            requireHeader(source, lines);
            while (lines.hasNext()) {
                imported.add(register(source, parser, lines.next(), imported.size()));
            }
        } catch (IOException e) {
            throw unreadable(source, e);
        } catch (UncheckedIOException e) {
            // The parser reports a malformed line (an unclosed quote, for example) as a CSVException
            // wrapped in an UncheckedIOException: that is a rejected line, not a file that cannot be read.
            if (e.getCause() instanceof CSVException malformed) {
                throw rejected(source, imported.size(), "malformed CSV: " + malformed.getMessage(), malformed);
            }
            throw unreadable(source, e.getCause());
        }
        LOGGER.info("Imported {} students from {}", imported.size(), source.getAbsolutePath());
        return imported;
    }

    private static void requireHeader(File source, Iterator<CSVRecord> lines) {
        if (!lines.hasNext()) {
            throw rejected(source, 0, "the header line " + String.join(",", HEADER) + " is missing", null);
        }
        List<String> header = new ArrayList<>(lines.next().toList());
        if (header.get(0).startsWith(BYTE_ORDER_MARK)) {
            header.set(0, header.get(0).substring(BYTE_ORDER_MARK.length()));
        }
        if (!header.equals(HEADER)) {
            throw rejected(source, 0, "wrong header " + header + ", expected " + String.join(",", HEADER), null);
        }
    }

    private Student register(File source, CSVParser parser, CSVRecord line, int importedSoFar) {
        long lineNumber = parser.getCurrentLineNumber();
        if (line.size() != HEADER.size()) {
            throw rejected(source, importedSoFar, "line " + lineNumber + " has " + line.size()
                    + " values instead of " + HEADER.size() + " (" + String.join(",", HEADER) + ")", null);
        }
        try {
            return system.createStudent(line.get(0), line.get(1), line.get(2));
        } catch (IllegalArgumentException e) {
            throw rejected(source, importedSoFar, "line " + lineNumber + " was rejected: " + e.getMessage(), e);
        }
    }

    private static IllegalArgumentException rejected(File source, int importedSoFar, String reason, Exception cause) {
        LOGGER.warn("Import of {} stopped, {} students imported before: {}",
                source.getAbsolutePath(), importedSoFar, reason);
        return new IllegalArgumentException("Cannot import " + source + ": " + reason, cause);
    }

    private static UncheckedIOException unreadable(File source, IOException cause) {
        LOGGER.warn("Import of {} failed, the file cannot be read: {}", source.getAbsolutePath(), cause.toString());
        return new UncheckedIOException("Cannot read the students file " + source, cause);
    }
}
