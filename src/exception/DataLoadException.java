package exception;

// Thrown when a data file cannot be read (or written), for example when the
// file is locked, the folder is missing, or the disk is full.
// We use the same exception for saving, to keep the number of classes small.
// INHERITANCE: a custom exception that extends Exception, used for file reading and writing problems.
public class DataLoadException extends Exception {

    private static final long serialVersionUID = 1;

    public DataLoadException(String message) {
        super(message);
    }
}
