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
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Registers students from a UTF-8 CSV file, with Apache Commons CSV.
 * <p>
 * The first line must be the header {@code id,name,email}; every other line is one student.
 * Empty lines, and lines that hold only spaces or tabs, are ignored, and values may be quoted.
 * Each line is registered through {@link WorkshopSystem#createStudent}, so exactly the same
 * rules apply as for a direct call. The lines are processed in file order and the import stops
 * at the first rejected line: the students of the lines before it stay registered.
 * <p>
 * The file is decoded strictly: bytes that are not valid UTF-8 make the file unreadable
 * instead of being silently replaced.
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
     * @throws UncheckedIOException     if the file cannot be read: missing, a folder, an invalid path,
     *                                  or content that is not valid UTF-8
     */
    List<Student> importStudents(File source) {
        Path path = pathOf(source);
        List<Student> imported = new ArrayList<>();
        // Files.newBufferedReader decodes strictly: invalid UTF-8 throws a MalformedInputException,
        // which is an IOException. Both resources are closed, so the file is never left locked.
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = CSVParser.parse(reader, FORMAT)) {
            Iterator<CSVRecord> lines = parser.iterator();
            requireHeader(source, lines);
            CSVRecord line;
            while ((line = nextNonBlankLine(lines)) != null) {
                imported.add(register(source, parser, line, imported.size()));
            }
        } catch (IOException e) {
            throw unreadable(source, e);
        } catch (UncheckedIOException e) {
            // The parser reports a malformed line (an unclosed quote, for example) as a CSVException
            // wrapped in an UncheckedIOException: that is a rejected line, not a file that cannot be read.
            // Any other cause (invalid UTF-8, a read error) means that the file cannot be read.
            if (e.getCause() instanceof CSVException malformed) {
                throw rejected(source, imported.size(), "malformed CSV: " + malformed.getMessage(), malformed);
            }
            throw unreadable(source, e.getCause());
        }
        LOGGER.info("Imported {} students from {}", imported.size(), source.getAbsolutePath());
        return imported;
    }

    /**
     * File.toPath() throws an InvalidPathException (an IllegalArgumentException) for a name that the
     * file system cannot hold, such as bad&lt;name&gt;.csv on Windows. Such a file cannot be read,
     * so it is reported like any other read failure, not like a rejected line.
     */
    private static Path pathOf(File source) {
        try {
            return source.toPath();
        } catch (InvalidPathException e) {
            throw unreadable(source, new IOException("Invalid file path: " + source, e));
        }
    }

    private static void requireHeader(File source, Iterator<CSVRecord> lines) {
        CSVRecord headerLine = nextNonBlankLine(lines);
        if (headerLine == null) {
            throw rejected(source, 0, "the header line " + String.join(",", HEADER) + " is missing", null);
        }
        List<String> header = new ArrayList<>(headerLine.toList());
        if (header.get(0).startsWith(BYTE_ORDER_MARK)) {
            header.set(0, header.get(0).substring(BYTE_ORDER_MARK.length()));
        }
        if (!header.equals(HEADER)) {
            throw rejected(source, 0, "wrong header " + header + ", expected " + String.join(",", HEADER), null);
        }
    }

    /** The next line that is not blank (see {@link #isBlank}), or null at the end of the file. */
    private static CSVRecord nextNonBlankLine(Iterator<CSVRecord> lines) {
        while (lines.hasNext()) {
            CSVRecord line = lines.next();
            if (!isBlank(line)) {
                return line;
            }
        }
        return null;
    }

    /**
     * True for a line that holds only spaces or tabs. The parser already skips empty lines; it reads
     * a blank line as one blank value, and such a line is skipped the same way. A line with commas
     * (", ," for example) has several values: it is not blank, and it is rejected like any other line.
     */
    private static boolean isBlank(CSVRecord line) {
        return line.size() == 1 && line.get(0).isBlank();
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
