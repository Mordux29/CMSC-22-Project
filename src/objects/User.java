package objects;

//abstract class for manager and clerk
// ABSTRACTION: User is abstract, so only a Manager or a Clerk can exist, never a plain User.
// INHERITANCE: Manager and Clerk extend this class and share the username/password code written here.
// POLYMORPHISM: the rest of the program only asks "user.canEditProducts()". It does not need to know if
// the user is a Manager or a Clerk, because each one answers in its own way (see the abstract methods below).
public abstract class User {
	
    // ENCAPSULATION: the password is private. Nobody can read it, they can only ask checkPassword(...).
    private String username;
    private String password;  
    private String phone = "";      // optional contact details (shown on the Accounts page)
    private String location = "";

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() { return username; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public void setPassword(String password) { this.password = password; }

    //Returns if its a Manager or Clerk object as a string 
    public abstract String getRole();

    //Checks if the user has abilities to edit, delete, and add products base on the objects role
    public abstract boolean canEditProducts();

    //Checks also if the user can restock or set reorder level for products
    public abstract boolean canRestock();

    //Checks if the user can record sales and void transactions
    public abstract boolean canProcessTransactions();

    //Checks if the user can see the transactions and dashboards of ALL clerks
    public abstract boolean canViewAllTransactions();

    //Checks if the user can create Clerk accounts (only the Manager can)
    public abstract boolean canManageAccounts();

    //Password validator which compares the objects password to the inputed password to see if its the same
    //returns true if it equaled and false if not
    public boolean checkPassword(String attempt) {
        return password.equals(attempt);
    }

    // Saves the users Username, password, and role in one string
    // and its formatted with a separator "|" Username|Password|Role|Phone|Location
    // PERSISTENCE: one line of text used to save this account in users.txt.
    public String toFileString() {
        return username + "|" + password + "|" + getRole() + "|" + phone + "|" + location;
    }
}
