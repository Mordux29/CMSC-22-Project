package dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import managers.InventoryManager;
import managers.TransactionManager;
import objects.Transaction;

// What one clerk sees: today's sales, the wallet,
// stock warnings, and their own transactions.
// INHERITANCE: extends Dashboard, so it reuses the wallet and stock code and only adds what one clerk sees.
public class ClerkDashboard extends Dashboard {

    private String clerkUsername;

    public ClerkDashboard(String clerkUsername, TransactionManager transactions,
                          InventoryManager inventory) {
        // super(...) calls the Dashboard constructor to set up the shared fields.
        super(transactions, inventory);
        this.clerkUsername = clerkUsername;
    }

    public String getClerkUsername() { return clerkUsername; }

    // Every transaction this clerk recorded.
    public ArrayList<Transaction> getMyTransactions() {
        return getTransactions().getByClerk(clerkUsername);
    }

    // @Override: the Clerk version counts only this clerk's sales (POLYMORPHISM: same method name, different behaviour).
    @Override
    public int getSalesCountOn(LocalDate date) {
        return getTransactions().getActiveSalesOn(getMyTransactions(), date).size();
    }

    @Override
    public double getSalesTotalOn(LocalDate date) {
        return getTransactions().sumAmounts(getTransactions().getActiveSalesOn(getMyTransactions(), date));
    }

    // The last "count" transactions of this clerk, newest first.
    @Override
    public ArrayList<Transaction> getRecentTransactions(int count) {
        ArrayList<Transaction> mine = getMyTransactions();
        ArrayList<Transaction> result = new ArrayList<Transaction>();
        for (int i = mine.size() - 1; i >= 0 && result.size() < count; i--) {
            result.add(mine.get(i));
        }
        return result;
    }

    @Override
    public String getTitle() {
        return "CLERK DASHBOARD: " + clerkUsername;
    }

    @Override
    public ArrayList<String> getSummary() {
        ArrayList<String> lines = new ArrayList<String>();
        lines.add("Today's sales: " + getTodaySalesCount() + " (total " + money(getTodaySalesTotal()) + ")");
        lines.add("Wallet balance: " + money(getWalletBalance()));
        lines.add("Low-stock items: " + getLowStockCount());
        lines.add("Expiring within " + EXPIRING_DAYS + " days (or expired): " + getExpiringSoonCount());
        return lines;
    }

    private String money(double amount) {
        return String.format("%.2f", amount);
    }
}
