package patmal.workshops.logic;

import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Workshop;
import patmal.workshops.api.model.Team;
import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.model.InstructorImpl;
import patmal.workshops.model.StudentImpl;
import patmal.workshops.model.WorkshopImpl;
import patmal.workshops.model.TeamImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of the WorkshopSystem interface.
 * Manages instructors, students, workshops, and teams in memory using Maps.
 */
public class WorkshopSystemImpl implements WorkshopSystem {
    
    private Map<String, Instructor> instructors;
    private Map<String, Student> students;
    private Map<Integer, Workshop> workshops;
    private Map<Integer, Team> teams;
    
    private Integer workshopIdCounter;
    private Integer teamIdCounter;
    
    /**
     * Constructs a new WorkshopSystemImpl with empty collections
     * and ID counters initialized to 1.
     */
    public WorkshopSystemImpl() {
        this.instructors = new HashMap<>();
        this.students = new HashMap<>();
        this.workshops = new HashMap<>();
        this.teams = new HashMap<>();
        this.workshopIdCounter = 1;
        this.teamIdCounter = 1;
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
     * @throws IllegalArgumentException if an instructor with the given ID already exists
     */
    @Override
    public Instructor createInstructor(String id, String name, String email) {
        Objects.requireNonNull(id, "Instructor ID cannot be null");
        Objects.requireNonNull(name, "Instructor name cannot be null");
        Objects.requireNonNull(email, "Instructor email cannot be null");
        
        if (instructors.containsKey(id)) {
            throw new IllegalArgumentException("Instructor with ID '" + id + "' already exists");
        }
        
        Instructor instructor = new InstructorImpl(id, name, email, new ArrayList<>());
        instructors.put(id, instructor);
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
     * @throws IllegalArgumentException if a student with the given ID already exists
     */
    @Override
    public Student createStudent(String id, String name, String email) {
        Objects.requireNonNull(id, "Student ID cannot be null");
        Objects.requireNonNull(name, "Student name cannot be null");
        Objects.requireNonNull(email, "Student email cannot be null");
        
        if (students.containsKey(id)) {
            throw new IllegalArgumentException("Student with ID '" + id + "' already exists");
        }
        
        Student student = new StudentImpl(id, name, email, null);
        students.put(id, student);
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
        Objects.requireNonNull(name, "Workshop name cannot be null");
        Objects.requireNonNull(description, "Workshop description cannot be null");
        
        Integer workshopId = workshopIdCounter;
        workshopIdCounter++;
        
        Workshop workshop = new WorkshopImpl(workshopId, name, description, null);
        workshops.put(workshopId, workshop);
        return workshop;
    }
    
    /**
     * Associates a workshop with an instructor by updating both sides of the relationship.
     * Sets the instructor on the workshop and adds the workshop to the instructor's list.
     *
     * @param instructor the instructor to associate
     * @param workshop   the workshop to associate
     * @throws IllegalArgumentException if instructor or workshop is null
     */
    @Override
    public void associateWorkshopWithInstructor(Instructor instructor, Workshop workshop) {
        Objects.requireNonNull(instructor, "Instructor cannot be null");
        Objects.requireNonNull(workshop, "Workshop cannot be null");
        
        if (workshop instanceof WorkshopImpl) {
            ((WorkshopImpl) workshop).setInstructor(instructor);
        }
        
        if (instructor instanceof InstructorImpl) {
            ((InstructorImpl) instructor).addWorkshop(workshop);
        }
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
     */
    @Override
    public Team createTeam(String name, Workshop workshop, List<String> memberIds) {
        Objects.requireNonNull(name, "Team name cannot be null");
        Objects.requireNonNull(workshop, "Workshop cannot be null");
        Objects.requireNonNull(memberIds, "Member IDs list cannot be null");
        
        // Validation phase - check all conditions BEFORE modifying any state
        
        // Check for duplicate member IDs
        if (memberIds.size() != memberIds.stream().distinct().count()) {
            throw new IllegalArgumentException("Member IDs list contains duplicates");
        }
        
        // Collect all student objects and validate they exist and are free
        List<Student> teamMembers = new ArrayList<>();
        for (String memberId : memberIds) {
            Student student = students.get(memberId);
            if (student == null) {
                throw new IllegalArgumentException("Student with ID '" + memberId + "' does not exist");
            }
            if (student.getTeamName() != null) {
                throw new IllegalArgumentException("Student with ID '" + memberId + "' is already in a team");
            }
            teamMembers.add(student);
        }
        
        // All validation passed - now create the team and update state
        Integer teamId = teamIdCounter;
        teamIdCounter++;
        
        Team team = new TeamImpl(teamId, name, teamMembers, workshop);
        teams.put(teamId, team);
        
        // Link each student to the team
        for (Student student : teamMembers) {
            if (student instanceof StudentImpl) {
                ((StudentImpl) student).setTeam(team);
            }
        }
        
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
}
