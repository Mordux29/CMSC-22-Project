package exception;

// ValidationException is thrown when the user inputed a invalid string,integer, or value and when
// a user isn't allowed to access a function, resulting things that will break the code so instead
// of breaking this exception will be thrown
// and it will return a error message containing the where it went wrong
// INHERITANCE: our own exception type extends the built-in Exception class.
// EXCEPTION HANDLING: code that finds bad input "throws" it, and the screen "catches" it to show the message.
public class ValidationException extends Exception {

    private static final long serialVersionUID = 1;

    //Prints what went wrong with the input of the user
    public ValidationException(String message) {
        super(message); // pass the message to the parent class (Exception)
    }
}
