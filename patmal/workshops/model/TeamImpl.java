package patmal.workshops.model;

import patmal.workshops.api.model.Team;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Workshop;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of the Team interface.
 * Represents a team consisting of multiple students.
 */
public class TeamImpl implements Team {
    
    private final Integer id;
    private final String name;
    private final List<Student> students;
    private final Workshop workshop;
    
    /**
     * Constructs a TeamImpl with the given parameters.
     *
     * @param id       the unique identifier for the team
     * @param name     the name of the team
     * @param students the list of students that are members of this team
     * @param workshop the workshop that this team participates in
     */
    public TeamImpl(Integer id, String name, List<Student> students, Workshop workshop) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.students = new ArrayList<>(Objects.requireNonNull(students, "Students list cannot be null"));
        this.workshop = Objects.requireNonNull(workshop, "Workshop cannot be null");
    }
    
    @Override
    public Integer getId() {
        return id;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public List<Student> getStudents() {
        return new ArrayList<>(students);
    }
    
    @Override
    public Workshop getWorkshop() {
        return workshop;
    }
    
    @Override
    public String toString() {
        return "TeamImpl{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", studentsCount=" + students.size() +
                '}';
    }
}
