package patmal.workshops.student.impl.app;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Workshop;
import patmal.workshops.student.impl.logic.WorkshopSystemImpl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * workshop-cli: fills a WorkshopSystem with demo data and exports it to JSON files.
 * <p>
 * Usage: {@code java -jar workshop-cli.jar [students.csv]}
 * <p>
 * The students are imported from the CSV file given as argument or, without an argument, from the
 * students.csv bundled in the application. The files instructors.json, students.json and
 * teams.json are written to the current folder.
 */
public class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    private static final String BUNDLED_STUDENTS = "/students.csv";

    private static final int TEAM_SIZE = 2;

    public static void main(String[] args) {
        try {
            run(args);
        } catch (IllegalArgumentException | UncheckedIOException e) {
            // A wrong argument or a CSV file that is missing or rejected: no stack trace, a clear message
            LOGGER.error("workshop-cli failed: {}", e.getMessage());
            System.exit(1);
        }
    }

    private static void run(String[] args) {
        if (args.length > 1) {
            throw new IllegalArgumentException("Usage: java -jar workshop-cli.jar [students.csv]");
        }
        WorkshopSystem system = new WorkshopSystemImpl();

        // Instructors and workshops: Dana teaches two workshops, Yossi one (which gets no team)
        Instructor dana = system.createInstructor("i1", "Dana Levi", "dana.levi@univ.edu");
        Instructor yossi = system.createInstructor("i2", "Yossi Mizrahi", "yossi.mizrahi@univ.edu");
        Workshop java = system.createWorkshop("Java Workshop", "Object-oriented programming in Java");
        Workshop maven = system.createWorkshop("Maven Workshop", "Multi-module builds with Maven");
        Workshop web = system.createWorkshop("Web Workshop", "HTML, CSS and JavaScript basics");
        system.associateWorkshopWithInstructor(dana, java);
        system.associateWorkshopWithInstructor(dana, maven);
        system.associateWorkshopWithInstructor(yossi, web);

        // Students, from the CSV file
        List<Student> students = importStudents(system, args);

        // Teams of two, in file order; the remaining students stay free
        List<String> studentIds = new ArrayList<>();
        for (Student student : students) {
            studentIds.add(student.getId());
        }
        createTeam(system, "Alpha", java, studentIds, 0);
        createTeam(system, "Beta", maven, studentIds, TEAM_SIZE);

        LOGGER.info("Demo data ready: {} instructors, {} workshops, {} students ({} free), {} teams",
                system.getInstructors().size(), system.getWorkshops().size(), system.getStudents().size(),
                system.getFreeStudents().size(), system.getTeams().size());

        // Exports, to the current folder
        system.exportInstructorsDataToJson(new File("instructors.json"));
        system.exportStudentsDataToJson(new File("students.json"));
        system.exportTeamsDataToJson(new File("teams.json"));
        LOGGER.info("JSON files written to {}", new File("").getAbsolutePath());
    }

    /**
     * Imports the students of the file given as argument or, without an argument, of the bundled file.
     */
    private static List<Student> importStudents(WorkshopSystem system, String[] args) {
        if (args.length == 1) {
            File source = new File(args[0]);
            LOGGER.info("Importing the students of {}", source.getAbsolutePath());
            return system.importStudentDataFromFile(source);
        }
        LOGGER.info("No CSV file given: importing the demo students bundled in the application");
        return system.importStudentDataFromFile(copyOfBundledStudents());
    }

    /**
     * importStudentDataFromFile reads a File, and a resource inside the jar is not one:
     * the bundled students.csv is copied to a temporary file, deleted when the application ends.
     */
    private static File copyOfBundledStudents() {
        try (InputStream bundled = Main.class.getResourceAsStream(BUNDLED_STUDENTS)) {
            if (bundled == null) {
                throw new IllegalStateException(BUNDLED_STUDENTS + " is missing from the application");
            }
            Path copy = Files.createTempFile("workshop-cli-students-", ".csv");
            copy.toFile().deleteOnExit();
            Files.copy(bundled, copy, StandardCopyOption.REPLACE_EXISTING);
            return copy.toFile();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot copy the bundled " + BUNDLED_STUDENTS, e);
        }
    }

    /**
     * Creates a team with the next TEAM_SIZE students, starting at {@code from}, as long as at
     * least one student stays free after it.
     */
    private static void createTeam(WorkshopSystem system, String name, Workshop workshop,
                                   List<String> studentIds, int from) {
        int to = from + TEAM_SIZE;
        if (to >= studentIds.size()) {
            LOGGER.warn("Team {} is not created: not enough students to fill it and keep one free", name);
            return;
        }
        system.createTeam(name, workshop, studentIds.subList(from, to));
    }
}
