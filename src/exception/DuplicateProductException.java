package exception;

// Thrown when we try to add a product whose ID or name already exists. Basically catching
// duplicate names and id
// INHERITANCE: this extends ValidationException (not Exception), so any code that catches ValidationException
// also catches this one. It is a more specific kind of validation error.
public class DuplicateProductException extends ValidationException {

    private static final long serialVersionUID = 1;

    public DuplicateProductException(String message) {
        super(message);
    }
}
