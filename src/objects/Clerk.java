package objects;

//CAN ONLY VIEW PRODUCTS AND INVENTORY HENCE IT RETURNS FALSE ON canEDITPRODUCTS AND canRESTOCK
// INHERITANCE: Clerk extends User, so it already has a username, password and the other shared code.
// It only decides what this kind of user is allowed to do.
public class Clerk extends User {

    public Clerk(String username, String password) {
        // super(...) passes the username and password to the parent class (User).
        super(username, password);
    }

    // @Override: fills in the abstract methods that User left empty (POLYMORPHISM).
    @Override
    public String getRole() { return "Clerk"; }

    @Override
    public boolean canEditProducts() { return false; }

    @Override
    public boolean canRestock() { return false; }

    @Override
    public boolean canProcessTransactions() { return true; }

    @Override
    public boolean canViewAllTransactions() { return false; }

    @Override
    public boolean canManageAccounts() { return false; }
}
