package dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import managers.InventoryManager;
import managers.TransactionManager;
import objects.InventoryItem;
import objects.Transaction;

// Dashboard is the ABSTRACT parent of ClerkDashboard and ManagerDashboard.
// A dashboard only GATHERS information (numbers and text lines). It never
// prints anything, so the console now (and a JavaFX screen later) can show
// the same data in their own way.
// ABSTRACTION: the shared code is written here once. The abstract methods below are only declared, and each child
// dashboard writes its own version. INHERITANCE: ClerkDashboard and ManagerDashboard extend this class.
public abstract class Dashboard {

    // How many days ahead counts as "expiring soon".
    public static final int EXPIRING_DAYS = 3;

    // ENCAPSULATION: private fields; children use the protected getter below.
    private TransactionManager transactions;
    private InventoryManager inventory;

    public Dashboard(TransactionManager transactions, InventoryManager inventory) {
        this.transactions = transactions;
        this.inventory = inventory;
    }

    protected TransactionManager getTransactions() { return transactions; }

    public double getWalletBalance() {
        return transactions.getWallet().getBalance();
    }

    public int getLowStockCount() {
        return inventory.getLowStockItems().size();
    }

    public int getExpiringSoonCount() {
        return inventory.getExpiringSoon(EXPIRING_DAYS).size();
    }

    // The items at or below their reorder level (for a "needs attention" list).
    public ArrayList<InventoryItem> getLowStockItems() {
        return inventory.getLowStockItems();
    }

    // The items that are expired or expire soon.
    public ArrayList<InventoryItem> getExpiringSoonItems() {
        return inventory.getExpiringSoon(EXPIRING_DAYS);
    }

    // Each kind of dashboard decides WHOSE sales it counts (one clerk's or everybody's).

    // How many sales (voided ones left out) were made on a date.
    public abstract int getSalesCountOn(LocalDate date);

    // The money of the sales (voided ones left out) made on a date.
    public abstract double getSalesTotalOn(LocalDate date);

    // The last "count" transactions, newest first.
    public abstract ArrayList<Transaction> getRecentTransactions(int count);

    public int getTodaySalesCount() {
        return getSalesCountOn(LocalDate.now());
    }

    public double getTodaySalesTotal() {
        return getSalesTotalOn(LocalDate.now());
    }

    // The title of this dashboard, for example "CLERK DASHBOARD: clerk".
    public abstract String getTitle();

    // The lines of text this dashboard wants to show. Each kind of dashboard writes its own.
    public abstract ArrayList<String> getSummary();
}
