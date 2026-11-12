package patmal.workshops.student.impl.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Person;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Team;
import patmal.workshops.api.model.Workshop;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes the data of a {@link WorkshopSystem} to JSON files, with Jackson.
 * <p>
 * The model is a graph with cycles (a student knows its team and a team knows its students),
 * so it is never given to Jackson directly. Each export first copies the data it needs into the
 * small records at the bottom of this class, which only point down the tree, and Jackson writes
 * those records. A record type only has the fields of its own level, so no object in the file
 * can point back to one of its ancestors.
 * <p>
 * An export only reads the system, so it never changes it.
 */
final class JsonExporter {

    private static final Logger LOGGER = LogManager.getLogger(JsonExporter.class);

    /** Thread-safe and costly to create, so one writer is shared. Pretty-printed to keep the files readable. */
    private static final ObjectWriter WRITER = new ObjectMapper().writerWithDefaultPrettyPrinter();

    private final WorkshopSystem system;

    JsonExporter(WorkshopSystem system) {
        this.system = system;
    }

    /**
     * Writes every instructor, with its workshops, each workshop with its teams,
     * and each team with the details of its students.
     */
    void exportInstructors(File target) {
        Map<Integer, List<Team>> teamsByWorkshop = teamsByWorkshopId();
        List<InstructorJson> instructors = new ArrayList<>();
        for (Instructor instructor : system.getInstructors()) {
            List<InstructorWorkshopJson> workshops = new ArrayList<>();
            for (Workshop workshop : instructor.getWorkshops()) {
                List<WorkshopTeamJson> teams = new ArrayList<>();
                for (Team team : teamsByWorkshop.getOrDefault(workshop.getId(), List.of())) {
                    teams.add(new WorkshopTeamJson(team.getId(), team.getName(), detailsOf(team.getStudents())));
                }
                workshops.add(new InstructorWorkshopJson(
                        workshop.getId(), workshop.getName(), workshop.getDescription(), teams));
            }
            instructors.add(new InstructorJson(
                    instructor.getId(), instructor.getName(), instructor.getEmail(), workshops));
        }
        write(target, instructors, "instructors");
    }

    /**
     * Writes every student, free or not, with its team (and the ids of all the team's students),
     * the team's workshop and the workshop's instructor. A free student has a null team.
     */
    void exportStudents(File target) {
        Map<String, Team> teamsByStudent = teamsByStudentId();
        List<StudentJson> students = new ArrayList<>();
        for (Student student : system.getStudents()) {
            Team team = teamsByStudent.get(student.getId());
            StudentTeamJson teamJson = null;
            if (team != null) {
                List<String> studentIds = new ArrayList<>();
                for (Student member : team.getStudents()) {
                    studentIds.add(member.getId());
                }
                teamJson = new StudentTeamJson(team.getId(), team.getName(), studentIds, workshopOf(team));
            }
            students.add(new StudentJson(student.getId(), student.getName(), student.getEmail(), teamJson));
        }
        write(target, students, "students");
    }

    /**
     * Writes every team, with the details of its students, its workshop and the workshop's instructor.
     */
    void exportTeams(File target) {
        List<TeamJson> teams = new ArrayList<>();
        for (Team team : system.getTeams()) {
            teams.add(new TeamJson(team.getId(), team.getName(), detailsOf(team.getStudents()), workshopOf(team)));
        }
        write(target, teams, "teams");
    }

    /**
     * Writes the records as a JSON array in UTF-8. An existing file is overwritten and a missing
     * one is created; its folder must already exist.
     *
     * @throws UncheckedIOException if the file cannot be written, including a path that the
     *                              file system rejects
     */
    private static void write(File target, List<?> records, String what) {
        Path path = pathOf(target, what);
        try (Writer out = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            WRITER.writeValue(out, records);
        } catch (IOException e) {
            throw writeFailed(target, what, e);
        }
        LOGGER.info("Exported {} {} to {}", records.size(), what, target.getAbsolutePath());
    }

    /**
     * File.toPath() throws an InvalidPathException (an IllegalArgumentException) for a name that the
     * file system cannot hold, such as bad&lt;name&gt;.json on Windows. Such a file cannot be written,
     * so it is reported like any other write failure.
     */
    private static Path pathOf(File target, String what) {
        try {
            return target.toPath();
        } catch (InvalidPathException e) {
            throw writeFailed(target, what, new IOException("Invalid file path: " + target, e));
        }
    }

    /** Logs a failed export at WARN level and returns the exception that reports it, for the caller to throw. */
    private static UncheckedIOException writeFailed(File target, String what, IOException cause) {
        LOGGER.warn("Export of the {} to {} failed: {}", what, target.getAbsolutePath(), cause.toString());
        return new UncheckedIOException("Cannot write the " + what + " export to " + target, cause);
    }

    /** The teams of each workshop, in creation order (a workshop has no getter for its teams). */
    private Map<Integer, List<Team>> teamsByWorkshopId() {
        Map<Integer, List<Team>> teamsByWorkshop = new HashMap<>();
        for (Team team : system.getTeams()) {
            teamsByWorkshop.computeIfAbsent(team.getWorkshop().getId(), id -> new ArrayList<>()).add(team);
        }
        return teamsByWorkshop;
    }

    /** The team of each student that is in a team (a student has no getter for its team). */
    private Map<String, Team> teamsByStudentId() {
        Map<String, Team> teamsByStudent = new HashMap<>();
        for (Team team : system.getTeams()) {
            for (Student student : team.getStudents()) {
                teamsByStudent.put(student.getId(), team);
            }
        }
        return teamsByStudent;
    }

    private static WorkshopJson workshopOf(Team team) {
        Workshop workshop = team.getWorkshop();
        Instructor instructor = workshop.getInstructor();
        return new WorkshopJson(workshop.getId(), workshop.getName(), workshop.getDescription(),
                instructor == null ? null : detailsOf(instructor));
    }

    private static List<PersonJson> detailsOf(List<? extends Person> people) {
        List<PersonJson> details = new ArrayList<>();
        for (Person person : people) {
            details.add(detailsOf(person));
        }
        return details;
    }

    private static PersonJson detailsOf(Person person) {
        return new PersonJson(person.getId(), person.getName(), person.getEmail());
    }

    // ---------------------------------------------------------------------------------------------
    // The shape of the files. Jackson writes each record as an object with exactly these fields
    // (ids of workshops and teams as numbers, a missing relation as null).
    // ---------------------------------------------------------------------------------------------

    /** The details of a person (instructor or student). */
    private record PersonJson(String id, String name, String email) {
    }

    // Instructors export: instructor -> workshops -> teams -> students

    private record InstructorJson(String id, String name, String email, List<InstructorWorkshopJson> workshops) {
    }

    private record InstructorWorkshopJson(Integer id, String name, String description, List<WorkshopTeamJson> teams) {
    }

    private record WorkshopTeamJson(Integer id, String name, List<PersonJson> students) {
    }

    // Students export: student -> team -> workshop -> instructor

    private record StudentJson(String id, String name, String email, StudentTeamJson team) {
    }

    private record StudentTeamJson(Integer id, String name, List<String> studentIds, WorkshopJson workshop) {
    }

    // Teams export: team -> students, and team -> workshop -> instructor

    private record TeamJson(Integer id, String name, List<PersonJson> students, WorkshopJson workshop) {
    }

    /** A workshop with its instructor (null when it has none), used by the students and teams exports. */
    private record WorkshopJson(Integer id, String name, String description, PersonJson instructor) {
    }
}
