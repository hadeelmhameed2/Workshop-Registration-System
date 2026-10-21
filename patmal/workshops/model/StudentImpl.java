package patmal.workshops.model;

import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Team;
import patmal.workshops.api.model.Instructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of the Student interface.
 * Represents a student enrolled in workshops.
 * Dynamically derives team information from the assigned Team object.
 */
public class StudentImpl implements Student {
    
    private final String id;
    private final String name;
    private final String email;
    private Team team;
    
    /**
     * Constructs a StudentImpl with the given parameters.
     *
     * @param id    the unique identifier for the student
     * @param name  the name of the student
     * @param email the email address of the student
     * @param team  the team this student belongs to, or null if the student is free
     */
    public StudentImpl(String id, String name, String email, Team team) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.team = team;
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getEmail() {
        return email;
    }
    
    @Override
    public String getTeamName() {
        return team != null ? team.getName() : null;
    }
    
    @Override
    public Instructor getInstructor() {
        return team != null ? team.getWorkshop().getInstructor() : null;
    }
    
    @Override
    public List<Student> getTeamMates() {
        if (team == null) {
            return new ArrayList<>();
        }
        
        List<Student> teamMates = new ArrayList<>(team.getStudents());
        teamMates.remove(this);
        return teamMates;
    }
    
    /**
     * Sets the team for this student.
     * Used internally by WorkshopSystemImpl.
     *
     * @param team the team to assign to this student, or null for a free student
     */
    public void setTeam(Team team) {
        this.team = team;
    }
    
    @Override
    public String toString() {
        return "StudentImpl{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", teamName=" + getTeamName() +
                '}';
    }
}
