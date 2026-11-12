package patmal.workshops.student.impl.model;

import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Workshop;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of the Instructor interface.
 * Represents an instructor who leads workshops.
 */
public class InstructorImpl implements Instructor {
    
    private final String id;
    private final String name;
    private final String email;
    private final List<Workshop> workshops;
    
    /**
     * Constructs an InstructorImpl with the given parameters.
     *
     * @param id        the unique identifier for the instructor
     * @param name      the name of the instructor
     * @param email     the email address of the instructor
     * @param workshops the list of workshops led by this instructor
     */
    public InstructorImpl(String id, String name, String email, List<Workshop> workshops) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.workshops = new ArrayList<>(Objects.requireNonNull(workshops, "Workshops list cannot be null"));
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
    public List<Workshop> getWorkshops() {
        return new ArrayList<>(workshops);
    }
    
    /**
     * Adds a workshop to this instructor's list.
     * Used internally by WorkshopSystemImpl.
     *
     * @param workshop the workshop to add
     */
    public void addWorkshop(Workshop workshop) {
        if (workshop != null && !workshops.contains(workshop)) {
            workshops.add(workshop);
        }
    }

    /**
     * Removes a workshop from this instructor's list.
     * Used internally by WorkshopSystemImpl when a workshop moves to another instructor.
     *
     * @param workshop the workshop to remove
     */
    public void removeWorkshop(Workshop workshop) {
        workshops.remove(workshop);
    }

    /**
     * Two instructors are equal when they have the same id - the id is the instructor's
     * identity in the system. The workshops list is mutable (and points back to this
     * instructor), so it is deliberately not compared.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Instructor other)) {
            return false;
        }
        return id.equals(other.getId());
    }

    /**
     * Consistent with {@link #equals(Object)}: based on the id only.
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "InstructorImpl{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
