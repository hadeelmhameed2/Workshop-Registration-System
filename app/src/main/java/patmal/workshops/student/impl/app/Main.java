package patmal.workshops.student.impl.app;

import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.student.impl.logic.WorkshopSystemImpl;

/**
 * Entry point of the workshop-cli application.
 * Placeholder for now: it only checks that the application is wired to the logic implementation.
 * The demo data, the JSON exports and the logging are not implemented yet.
 */
public class Main {

    public static void main(String[] args) {
        WorkshopSystem system = new WorkshopSystemImpl();
        System.out.println("workshop-cli: " + system.getClass().getSimpleName() + " is ready");
    }
}
