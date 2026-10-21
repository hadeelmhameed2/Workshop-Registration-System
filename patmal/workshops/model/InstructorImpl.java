package patmal.workshops.model;

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
    
    @Override
    public String toString() {
        return "InstructorImpl{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                '}';
    }
}
