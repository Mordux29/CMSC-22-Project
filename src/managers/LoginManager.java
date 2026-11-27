package managers;

import java.util.ArrayList;
import exception.ValidationException;

import objects.Clerk;
import objects.Manager;
import objects.User;

// AuthService keeps the list of user accounts and checks logins.
// This manager's job: the user accounts. It holds a list of User objects (each one is a Manager or a Clerk).
public class LoginManager {

    // The one account that exists from the very first run.
    public static final String DEFAULT_MANAGER_USERNAME = "manager";
    public static final String DEFAULT_MANAGER_PASSWORD = "manager123";

    private ArrayList<User> users;

    public LoginManager() {
        users = new ArrayList<User>();
    }

    public ArrayList<User> getUsers() { return users; }

    // Replaces the whole list (used when loading from a file).
    public void setUsers(ArrayList<User> users) { this.users = users; }

    // Returns the matching User if the username and password are right,
    // otherwise returns null.
    // The returned object is really a Manager or a Clerk, but we only see it
    // as a User, and its permission methods still behave correctly.
    public User login(String username, String password) {
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            if (u.getUsername().equalsIgnoreCase(username.trim()) && u.checkPassword(password)) {
                return u;
            }
        }
        return null;
    }

    // True if a Manager account exists.
    public boolean hasManager() {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getRole().equals("Manager")) {
                return true;
            }
        }
        return false;
    }

    // Makes sure the Manager account exists. The Manager is the only account that
    // is created automatically (manager / manager123), and only when there is none.
    // Returns true if it had to create it.
    public boolean ensureManager() {
        if (hasManager()) {
            return false;
        }
        users.add(new Manager(DEFAULT_MANAGER_USERNAME, DEFAULT_MANAGER_PASSWORD));
        return true;
    }

    // Creates a new CLERK account. There is no way to create a second Manager,
    // so only one Manager can ever exist.
    // Throws ValidationException if the username or password is empty or has the
    // "|" character (our file separator), if the password is too short, or if the
    // username is already taken.
    public User registerClerk(String username, String password) throws ValidationException {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("Username cannot be empty.");
        }
        if (password == null || password.isEmpty()) {
            throw new ValidationException("Password cannot be empty.");
        }
        username = username.trim();
        if (username.contains("|") || password.contains("|")) {
            throw new ValidationException("Username and password cannot contain the \"|\" character.");
        }
        if (password.length() < 4) {
            throw new ValidationException("Password must be at least 4 characters.");
        }
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUsername().equalsIgnoreCase(username)) {
                throw new ValidationException("The username \"" + username + "\" is already taken.");
            }
        }
        User clerk = new Clerk(username, password);
        users.add(clerk);
        return clerk;
    }

    // Same as registerClerk, and also saves the phone and location
    public User registerClerk(String username, String password, String phone, String location)
            throws ValidationException {
        checkContact(phone, location);
        User clerk = registerClerk(username, password);
        clerk.setPhone(phone == null ? "" : phone.trim());
        clerk.setLocation(location == null ? "" : location.trim());
        return clerk;
    }

    // Finds a Clerk by username (returns null if there is none)
    public User findClerk(String username) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getRole().equals("Clerk") && users.get(i).getUsername().equalsIgnoreCase(username.trim())) {
                return users.get(i);
            }
        }
        return null;
    }

    // Changes a Clerk's phone and location, and the password too if newPassword is not empty
    public void updateClerk(String username, String phone, String location, String newPassword)
            throws ValidationException {
        User clerk = findClerk(username);
        if (clerk == null) {
            throw new ValidationException("No clerk named " + username + ".");
        }
        checkContact(phone, location);
        if (newPassword != null && !newPassword.isEmpty()) {
            if (newPassword.contains("|")) {
                throw new ValidationException("Password cannot contain the \"|\" character.");
            }
            if (newPassword.length() < 4) {
                throw new ValidationException("Password must be at least 4 characters.");
            }
            clerk.setPassword(newPassword);
        }
        clerk.setPhone(phone == null ? "" : phone.trim());
        clerk.setLocation(location == null ? "" : location.trim());
    }

    // Removes a Clerk account (their transactions are not touched, so the history stays)
    public void removeClerk(String username) throws ValidationException {
        User clerk = findClerk(username);
        if (clerk == null) {
            throw new ValidationException("No clerk named " + username + ".");
        }
        users.remove(clerk);
    }

    // Makes sure the phone and location do not contain the "|" file separator
    private void checkContact(String phone, String location) throws ValidationException {
        if ((phone != null && phone.contains("|")) || (location != null && location.contains("|"))) {
            throw new ValidationException("Phone and location cannot contain the \"|\" character.");
        }
    }
}
