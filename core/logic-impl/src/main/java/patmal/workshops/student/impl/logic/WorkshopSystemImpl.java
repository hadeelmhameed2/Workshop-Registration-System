package patmal.workshops.student.impl.logic;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Workshop;
import patmal.workshops.api.model.Team;
import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.student.impl.model.InstructorImpl;
import patmal.workshops.student.impl.model.StudentImpl;
import patmal.workshops.student.impl.model.WorkshopImpl;
import patmal.workshops.student.impl.model.TeamImpl;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the WorkshopSystem interface.
 * Manages instructors, students, workshops, and teams in memory using Maps.
 * <p>
 * Every successful change is logged at INFO level and every rejected operation at WARN level.
 * The JSON exports and the CSV import are delegated to {@link JsonExporter} and {@link StudentCsvImporter}.
 */
public class WorkshopSystemImpl implements WorkshopSystem {

    private static final Logger LOGGER = LogManager.getLogger(WorkshopSystemImpl.class);

    private static final EmailValidator EMAIL_VALIDATOR = EmailValidator.getInstance();

    // The maps hold the concrete classes, so the system can call their setters
    // (setTeam, setInstructor, addWorkshop...) without instanceof checks and casts.
    // They are LinkedHashMaps: iteration follows insertion order, so all the getters
    // and the JSON exports list the objects in creation order.
    private final Map<String, InstructorImpl> instructors;
    private final Map<String, StudentImpl> students;
    private final Map<Integer, WorkshopImpl> workshops;
    private final Map<Integer, TeamImpl> teams;

    private int workshopIdCounter;
    private int teamIdCounter;

    private final JsonExporter jsonExporter;
    private final StudentCsvImporter csvImporter;

    /**
     * Constructs a new WorkshopSystemImpl with empty collections
     * and ID counters initialized to 1.
     */
    public WorkshopSystemImpl() {
        this.instructors = new LinkedHashMap<>();
        this.students = new LinkedHashMap<>();
        this.workshops = new LinkedHashMap<>();
        this.teams = new LinkedHashMap<>();
        this.workshopIdCounter = 1;
        this.teamIdCounter = 1;
        // Both only keep the reference: they read the system or call createStudent later, on demand
        this.jsonExporter = new JsonExporter(this);
        this.csvImporter = new StudentCsvImporter(this);
    }

    /**
     * Returns a defensive copy of all instructors.
     *
     * @return a list containing all instructors in the system
     */
    @Override
    public List<Instructor> getInstructors() {
        return new ArrayList<>(instructors.values());
    }

    /**
     * Retrieves an instructor by ID.
     *
     * @param id the instructor ID
     * @return the instructor with the given ID, or null if not found
     */
    @Override
    public Instructor getInstructor(String id) {
        return instructors.get(id);
    }

    /**
     * Returns a defensive copy of all students.
     *
     * @return a list containing all students in the system
     */
    @Override
    public List<Student> getStudents() {
        return new ArrayList<>(students.values());
    }

    /**
     * Retrieves a student by ID.
     *
     * @param id the student ID
     * @return the student with the given ID, or null if not found
     */
    @Override
    public Student getStudent(String id) {
        return students.get(id);
    }

    /**
     * Returns a defensive copy of all workshops.
     *
     * @return a list containing all workshops in the system
     */
    @Override
    public List<Workshop> getWorkshops() {
        return new ArrayList<>(workshops.values());
    }

    /**
     * Retrieves a workshop by ID.
     *
     * @param id the workshop ID
     * @return the workshop with the given ID, or null if not found
     */
    @Override
    public Workshop getWorkshop(Integer id) {
        return workshops.get(id);
    }

    /**
     * Returns a defensive copy of all teams.
     *
     * @return a list containing all teams in the system
     */
    @Override
    public List<Team> getTeams() {
        return new ArrayList<>(teams.values());
    }

    /**
     * Retrieves a team by ID.
     *
     * @param id the team ID
     * @return the team with the given ID, or null if not found
     */
    @Override
    public Team getTeam(Integer id) {
        return teams.get(id);
    }

    /**
     * Creates and adds a new instructor to the system.
     *
     * @param id    the unique identifier for the instructor
     * @param name  the name of the instructor
     * @param email the email of the instructor
     * @return the created instructor
     * @throws NullPointerException     if id or name is null
     * @throws IllegalArgumentException if the email is invalid or null,
     *                                  or if an instructor with the given ID already exists
     */
    @Override
    public Instructor createInstructor(String id, String name, String email) {
        requireNonNull(id, "Instructor ID cannot be null");
        requireNonNull(name, "Instructor name cannot be null");
        requireValidEmail(email, "instructor '" + id + "'");

        if (instructors.containsKey(id)) {
            throw rejected("Instructor with ID '" + id + "' already exists");
        }

        InstructorImpl instructor = new InstructorImpl(id, name, email, new ArrayList<>());
        instructors.put(id, instructor);
        LOGGER.info("Created instructor '{}' ({})", id, name);
        return instructor;
    }

    /**
     * Creates and adds a new student to the system.
     * The student is initially created as a free student (no team).
     *
     * @param id    the unique identifier for the student
     * @param name  the name of the student
     * @param email the email of the student
     * @return the created student
     * @throws NullPointerException     if id or name is null
     * @throws IllegalArgumentException if the email is invalid or null,
     *                                  or if a student with the given ID already exists
     */
    @Override
    public Student createStudent(String id, String name, String email) {
        requireNonNull(id, "Student ID cannot be null");
        requireNonNull(name, "Student name cannot be null");
        requireValidEmail(email, "student '" + id + "'");

        if (students.containsKey(id)) {
            throw rejected("Student with ID '" + id + "' already exists");
        }

        StudentImpl student = new StudentImpl(id, name, email, null);
        students.put(id, student);
        LOGGER.info("Created student '{}' ({})", id, name);
        return student;
    }

    /**
     * Creates and adds a new workshop to the system.
     * Assigns the next available workshop ID automatically.
     *
     * @param name        the name of the workshop
     * @param description the description of the workshop
     * @return the created workshop
     */
    @Override
    public Workshop createWorkshop(String name, String description) {
        requireNonNull(name, "Workshop name cannot be null");
        requireNonNull(description, "Workshop description cannot be null");

        Integer workshopId = workshopIdCounter;
        workshopIdCounter++;

        WorkshopImpl workshop = new WorkshopImpl(workshopId, name, description, null);
        workshops.put(workshopId, workshop);
        LOGGER.info("Created workshop {} ({})", workshopId, name);
        return workshop;
    }

    /**
     * Associates a workshop with an instructor by updating both sides of the relationship.
     * Sets the instructor on the workshop and adds the workshop to the instructor's list.
     * If the workshop was already taught by another instructor, it is removed from that
     * instructor's list, so both sides of the relation stay consistent.
     *
     * @param instructor the instructor to associate
     * @param workshop   the workshop to associate
     * @throws NullPointerException     if instructor or workshop is null
     * @throws IllegalArgumentException if instructor or workshop is not registered in this system
     */
    @Override
    public void associateWorkshopWithInstructor(Instructor instructor, Workshop workshop) {
        InstructorImpl newInstructor = requireRegisteredInstructor(instructor);
        WorkshopImpl registeredWorkshop = requireRegisteredWorkshop(workshop);

        Instructor previousInstructor = registeredWorkshop.getInstructor();
        if (previousInstructor == null) {
            LOGGER.info("Associated workshop {} with instructor '{}'", registeredWorkshop.getId(), newInstructor.getId());
        } else if (previousInstructor.getId().equals(newInstructor.getId())) {
            // Already associated: nothing to move, and the instructor's workshops keep their order
            LOGGER.info("Workshop {} is already associated with instructor '{}'",
                    registeredWorkshop.getId(), newInstructor.getId());
        } else {
            instructors.get(previousInstructor.getId()).removeWorkshop(registeredWorkshop);
            LOGGER.info("Moved workshop {} from instructor '{}' to instructor '{}'",
                    registeredWorkshop.getId(), previousInstructor.getId(), newInstructor.getId());
        }

        registeredWorkshop.setInstructor(newInstructor);
        newInstructor.addWorkshop(registeredWorkshop);
    }

    /**
     * Resolves the given instructor to the object registered in this system (matched by id).
     */
    private InstructorImpl requireRegisteredInstructor(Instructor instructor) {
        requireNonNull(instructor, "Instructor cannot be null");
        InstructorImpl registered = instructors.get(instructor.getId());
        if (registered == null) {
            throw rejected("Instructor with ID '" + instructor.getId() + "' does not exist");
        }
        return registered;
    }

    /**
     * Resolves the given workshop to the object registered in this system (matched by id).
     */
    private WorkshopImpl requireRegisteredWorkshop(Workshop workshop) {
        requireNonNull(workshop, "Workshop cannot be null");
        WorkshopImpl registered = workshops.get(workshop.getId());
        if (registered == null) {
            throw rejected("Workshop with ID '" + workshop.getId() + "' does not exist");
        }
        return registered;
    }

    /**
     * Creates a new team with the given members and associates it with a workshop.
     * Performs comprehensive validation before modifying any state.
     *
     * @param name      the name of the team
     * @param workshop  the workshop this team participates in
     * @param memberIds the list of student IDs that will be team members
     * @return the created team
     * @throws IllegalArgumentException if:
     *         - Any student ID in memberIds doesn't exist
     *         - memberIds contains duplicate IDs
     *         - Any student in memberIds is already in a team
     *         - The workshop is not registered in this system
     */
    @Override
    public Team createTeam(String name, Workshop workshop, List<String> memberIds) {
        requireNonNull(name, "Team name cannot be null");
        requireNonNull(memberIds, "Member IDs list cannot be null");

        // Validation phase - check all conditions BEFORE modifying any state

        WorkshopImpl registeredWorkshop = requireRegisteredWorkshop(workshop);

        // Check for duplicate member IDs
        if (memberIds.size() != memberIds.stream().distinct().count()) {
            throw rejected("Member IDs list contains duplicates: " + memberIds);
        }

        // Collect all student objects and validate they exist and are free
        List<StudentImpl> teamMembers = new ArrayList<>();
        for (String memberId : memberIds) {
            StudentImpl student = students.get(memberId);
            if (student == null) {
                throw rejected("Student with ID '" + memberId + "' does not exist");
            }
            if (student.getTeamName() != null) {
                throw rejected("Student with ID '" + memberId + "' is already in a team");
            }
            teamMembers.add(student);
        }

        // All validation passed - now create the team and update state
        Integer teamId = teamIdCounter;
        teamIdCounter++;

        TeamImpl team = new TeamImpl(teamId, name, new ArrayList<>(teamMembers), registeredWorkshop);
        teams.put(teamId, team);

        // Link each student to the team
        for (StudentImpl student : teamMembers) {
            student.setTeam(team);
        }

        LOGGER.info("Created team {} ({}) in workshop {} with students {}",
                teamId, name, registeredWorkshop.getId(), memberIds);
        return team;
    }

    /**
     * Returns all students who are not assigned to any team.
     * Returns an empty list if no free students exist.
     *
     * @return a defensive copy of the list of free students
     */
    @Override
    public List<Student> getFreeStudents() {
        List<Student> freeStudents = new ArrayList<>();
        for (Student student : students.values()) {
            if (student.getTeamName() == null) {
                freeStudents.add(student);
            }
        }
        return freeStudents;
    }

    /**
     * Returns all students enrolled in teams associated with a specific workshop.
     * Returns an empty list if the workshop doesn't exist or has no teams.
     *
     * @param workshopId the ID of the workshop
     * @return a defensive copy of the list of students in teams for this workshop
     */
    @Override
    public List<Student> getStudentsOfWorkshop(Integer workshopId) {
        if (workshopId == null || !workshops.containsKey(workshopId)) {
            return new ArrayList<>();
        }

        List<Student> workshopStudents = new ArrayList<>();
        for (Team team : teams.values()) {
            if (team.getWorkshop().getId().equals(workshopId)) {
                workshopStudents.addAll(team.getStudents());
            }
        }
        return workshopStudents;
    }

    /**
     * Returns all students enrolled in workshops taught by a specific instructor.
     * Returns an empty list if the instructor doesn't exist or has no students.
     *
     * @param instructorId the ID of the instructor
     * @return a defensive copy of the list of students for this instructor's workshops
     */
    @Override
    public List<Student> getStudentsOfInstructor(String instructorId) {
        Instructor instructor = instructors.get(instructorId);
        if (instructor == null) {
            return new ArrayList<>();
        }

        List<Student> instructorStudents = new ArrayList<>();
        for (Workshop workshop : instructor.getWorkshops()) {
            instructorStudents.addAll(getStudentsOfWorkshop(workshop.getId()));
        }
        return instructorStudents;
    }

    /**
     * Exports the instructors as a JSON array, in creation order: each instructor with its
     * workshops, each workshop with its teams, and each team with the details of its students.
     *
     * @param targetFileName the file to write (overwritten if it exists; its folder must exist)
     * @throws java.io.UncheckedIOException if the file cannot be written
     */
    @Override
    public void exportInstructorsDataToJson(File targetFileName) {
        requireNonNull(targetFileName, "Target file cannot be null");
        jsonExporter.exportInstructors(targetFileName);
    }

    /**
     * Exports the students as a JSON array, in creation order: each student with its team
     * (null for a free student), the team's workshop and the workshop's instructor.
     *
     * @param targetFileName the file to write (overwritten if it exists; its folder must exist)
     * @throws java.io.UncheckedIOException if the file cannot be written
     */
    @Override
    public void exportStudentsDataToJson(File targetFileName) {
        requireNonNull(targetFileName, "Target file cannot be null");
        jsonExporter.exportStudents(targetFileName);
    }

    /**
     * Exports the teams as a JSON array, in creation order: each team with the details of its
     * students, its workshop and the workshop's instructor.
     *
     * @param targetFileName the file to write (overwritten if it exists; its folder must exist)
     * @throws java.io.UncheckedIOException if the file cannot be written
     */
    @Override
    public void exportTeamsDataToJson(File targetFileName) {
        requireNonNull(targetFileName, "Target file cannot be null");
        jsonExporter.exportTeams(targetFileName);
    }

    /**
     * Registers the students listed in a UTF-8 CSV file with the header {@code id,name,email}.
     * Every line is registered exactly as if {@link #createStudent} had been called with its
     * values. The import stops at the first rejected line; the students of the lines before it
     * stay registered.
     *
     * @param source the CSV file to read
     * @return the students registered by this import, in file order
     * @throws IllegalArgumentException     if the header is missing or wrong, or at the first rejected line
     * @throws java.io.UncheckedIOException if the file cannot be read
     */
    @Override
    public List<Student> importStudentDataFromFile(File source) {
        requireNonNull(source, "Source file cannot be null");
        return csvImporter.importStudents(source);
    }

    /**
     * Rejects an e-mail address that is not valid (including null) with an IllegalArgumentException.
     *
     * @param email the address to check
     * @param owner who the address is for, used in the message
     */
    private static void requireValidEmail(String email, String owner) {
        if (!EMAIL_VALIDATOR.isValid(email)) {
            throw rejected("Invalid email address '" + email + "' for " + owner);
        }
    }

    /**
     * Logs a rejected operation at WARN level and returns the exception that rejects it,
     * for the caller to throw.
     */
    private static IllegalArgumentException rejected(String message) {
        LOGGER.warn("Rejected: {}", message);
        return new IllegalArgumentException(message);
    }

    /**
     * Like {@link java.util.Objects#requireNonNull(Object, String)}, but the rejection is
     * logged at WARN level as well.
     */
    private static void requireNonNull(Object value, String message) {
        if (value == null) {
            LOGGER.warn("Rejected: {}", message);
            throw new NullPointerException(message);
        }
    }
}
