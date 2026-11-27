package objects;

//HAVE ACCESS TO ALL THE FUNCTIONS HENCE ABSTRACT METHODS canEditProdcuts and canRestock returns TRUE
// INHERITANCE: Manager extends User, so it already has a username, password and the other shared code.
// It only decides what this kind of user is allowed to do.
public class Manager extends User {

    public Manager(String username, String password) {
        // super(...) passes the username and password to the parent class (User).
        super(username, password);
    }

    // @Override: fills in the abstract methods that User left empty (POLYMORPHISM).
    @Override
    public String getRole() { return "Manager"; }

    @Override
    public boolean canEditProducts() { return true; }

    @Override
    public boolean canRestock() { return true; }

    @Override
    public boolean canProcessTransactions() { return true; }

    @Override
    public boolean canViewAllTransactions() { return true; }

    @Override
    public boolean canManageAccounts() { return true; }
}
