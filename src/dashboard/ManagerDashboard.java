package dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import managers.InventoryManager;
import managers.LoginManager;
import managers.TransactionManager;
import objects.Transaction;
import objects.User;

// What the Manager sees: the whole shop's sales, the totals
// per clerk, the wallet, stock warnings, and a way to open any clerk's dashboard.
// INHERITANCE: extends Dashboard, so it reuses the wallet and stock code and only adds what a Manager sees.
public class ManagerDashboard extends Dashboard {

    private LoginManager users;
    private InventoryManager inventory;

    public ManagerDashboard(LoginManager users, TransactionManager transactions,
                            InventoryManager inventory) {
        // super(...) calls the Dashboard constructor to set up the shared fields.
        super(transactions, inventory);
        this.users = users;
        this.inventory = inventory;
    }

    // The usernames of every Clerk account.
    public ArrayList<String> getClerkNames() {
        ArrayList<String> names = new ArrayList<String>();
        ArrayList<User> all = users.getUsers();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getRole().equals("Clerk")) {
                names.add(all.get(i).getUsername());
            }
        }
        return names;
    }

    // Opens the dashboard of one clerk, or returns null if there is no such clerk.
    public ClerkDashboard getClerkDashboard(String username) {
        ArrayList<String> names = getClerkNames();
        for (int i = 0; i < names.size(); i++) {
            if (names.get(i).equalsIgnoreCase(username.trim())) {
                return new ClerkDashboard(names.get(i), getTransactions(), inventory);
            }
        }
        return null;
    }

    public ArrayList<Transaction> getSalesHistory() {
        return getTransactions().getByType("Sale");
    }

    // The sales of one clerk (this is the "monitor the clerk's sale history" feature).
    public ArrayList<Transaction> getSalesOfClerk(String username) {
        ArrayList<Transaction> result = new ArrayList<Transaction>();
        ArrayList<Transaction> mine = getTransactions().getByClerk(username);
        for (int i = 0; i < mine.size(); i++) {
            if (mine.get(i).getType().equals("Sale")) {
                result.add(mine.get(i));
            }
        }
        return result;
    }

    // Every clerk's sales (and the Manager's own) made on a date.
    // @Override: the Manager version counts everybody's sales (POLYMORPHISM: same method name, different behaviour).
    @Override
    public int getSalesCountOn(LocalDate date) {
        return getTransactions().getActiveSalesOn(getTransactions().getTransactions(), date).size();
    }

    @Override
    public double getSalesTotalOn(LocalDate date) {
        return getTransactions().sumAmounts(
                getTransactions().getActiveSalesOn(getTransactions().getTransactions(), date));
    }

    // The last "count" transactions of the whole shop, newest first.
    @Override
    public ArrayList<Transaction> getRecentTransactions(int count) {
        ArrayList<Transaction> all = getTransactions().getTransactions();
        ArrayList<Transaction> result = new ArrayList<Transaction>();
        for (int i = all.size() - 1; i >= 0 && result.size() < count; i--) {
            result.add(all.get(i));
        }
        return result;
    }

    public double getTotalSales() {
        return getTransactions().sumAmounts(getSalesHistory());
    }

    @Override
    public String getTitle() {
        return "MANAGER DASHBOARD";
    }

    @Override
    public ArrayList<String> getSummary() {
        ArrayList<String> lines = new ArrayList<String>();
        lines.add("Total sales: " + getTransactions().getActive(getSalesHistory()).size()
                + " (total " + money(getTotalSales()) + ")");
        lines.add("Today's sales: " + getTodaySalesCount() + " (total " + money(getTodaySalesTotal()) + ")");
        lines.add("Wallet balance: " + money(getWalletBalance()));
        lines.add("Low-stock items: " + getLowStockCount());
        lines.add("Expiring within " + EXPIRING_DAYS + " days (or expired): " + getExpiringSoonCount());
        lines.add("Sales per clerk:");
        ArrayList<String> clerks = getClerkNames();
        for (int i = 0; i < clerks.size(); i++) {
            ArrayList<Transaction> sales = getSalesOfClerk(clerks.get(i));
            lines.add("  " + clerks.get(i) + ": " + getTransactions().getActive(sales).size()
                    + " sales (total " + money(getTransactions().sumAmounts(sales)) + ")");
        }
        return lines;
    }

    private String money(double amount) {
        return String.format("%.2f", amount);
    }
}
