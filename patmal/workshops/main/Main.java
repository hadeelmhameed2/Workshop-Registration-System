package patmal.workshops.main;

import patmal.workshops.logic.WorkshopSystemImpl;
import patmal.workshops.api.logic.WorkshopSystem;

/**
 * Main entry point for the Workshop Registration System.
 */
public class Main {
    
    public static void main(String[] args) {
        System.out.println("=== Workshop Registration System ===");
        
        // Create the system
        WorkshopSystem system = new WorkshopSystemImpl();
        System.out.println("✅ System initialized successfully");
        
        // Test: Create an instructor
        var instructor = system.createInstructor("I001", "Dr. John Smith", "john@example.com");
        System.out.println("✅ Created instructor: " + instructor.getName());
        
        // Test: Create students
        var student1 = system.createStudent("S001", "Alice Johnson", "alice@example.com");
        var student2 = system.createStudent("S002", "Bob Davis", "bob@example.com");
        System.out.println("✅ Created students: " + student1.getName() + ", " + student2.getName());
        
        // Test: Create workshop
        var workshop = system.createWorkshop("Java Fundamentals", "Learn Java basics");
        System.out.println("✅ Created workshop: " + workshop.getName() + " (ID: " + workshop.getId() + ")");
        
        // Test: Associate instructor with workshop
        system.associateWorkshopWithInstructor(instructor, workshop);
        System.out.println("✅ Associated instructor with workshop");
        
        System.out.println("\n=== System is running correctly! ===");
    }
}
