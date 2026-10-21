package patmal.workshops.model;

import patmal.workshops.api.model.Workshop;
import patmal.workshops.api.model.Instructor;
import java.util.Objects;

/**
 * Implementation of the Workshop interface.
 * Represents a workshop event with instructor.
 */
public class WorkshopImpl implements Workshop {
    
    private final Integer id;
    private final String name;
    private final String description;
    private Instructor instructor;
    
    /**
     * Constructs a WorkshopImpl with the given parameters.
     *
     * @param id          the unique identifier for the workshop
     * @param name        the name of the workshop
     * @param description the description of the workshop
     * @param instructor  the instructor leading the workshop (may be null initially)
     */
    public WorkshopImpl(Integer id, String name, String description, Instructor instructor) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        this.instructor = instructor;
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
    public String getDescription() {
        return description;
    }
    
    @Override
    public Instructor getInstructor() {
        return instructor;
    }
    
    /**
     * Sets the instructor for this workshop.
     * Used internally by WorkshopSystemImpl.
     *
     * @param instructor the instructor to assign to this workshop
     */
    public void setInstructor(Instructor instructor) {
        this.instructor = instructor;
    }
    
    @Override
    public String toString() {
        return "WorkshopImpl{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
