package app;

import java.time.LocalDate;
import java.util.ArrayList;
import dashboard.ClerkDashboard;
import dashboard.Dashboard;
import dashboard.ManagerDashboard;
import exception.DataLoadException;
import exception.ValidationException;
import managers.LoginManager;
import managers.InventoryManager;
import managers.ProductManager;
import managers.TransactionManager;
import objects.Cart;
import objects.InventoryItem;
import objects.Product;
import objects.Sale;
import objects.SaleItem;
import objects.StockBatch;
import objects.Transaction;
import objects.User;
import persistence.FileHandler;

// This class is the single door to the whole backend. The screens (JavaFX or console) only talk to it, and it
// passes the work to the manager classes. ABSTRACTION: the screens do not need to know how products, files
// or transactions work inside. COMPOSITION: it HAS a FileHandler and one of each manager.
public class PorkingLotSystem {

    // Used when a product has no stock entry in inventory.txt yet.
    public static final double DEFAULT_QUANTITY = 50;
    public static final double DEFAULT_REORDER_LEVEL = 10;

    // ENCAPSULATION: private fields, the screens cannot touch them, they have to call the public methods below.
    private FileHandler fileHandler;
    private InventoryManager inventory;
    private ProductManager productManager;
    private LoginManager authService;
    private TransactionManager transactionManager;
    private User currentUser;   // null when nobody is logged in

    public PorkingLotSystem() {
        fileHandler = new FileHandler();
        inventory = new InventoryManager();
        productManager = new ProductManager(inventory);   // shares the same Inventory
        authService = new LoginManager();
        transactionManager = new TransactionManager(inventory, productManager);
        currentUser = null;
    }


    // Loads everything. Call this ONCE when the program starts.
    // Returns a list of messages (info and warnings) for the screen to show.
    // PERSISTENCE: loads the saved data from the data folder when the program starts.
    public ArrayList<String> startUp() {
        ArrayList<String> messages = new ArrayList<String>();
        boolean loadFailed = false;   // if a file could not be read, we must NOT overwrite it

        // ---------- 1. USERS (only the Manager is created automatically) ----------
        try {
            if (fileHandler.fileExists(FileHandler.USERS_FILE)) {
                authService.setUsers(fileHandler.loadUsers());
            }
        } catch (DataLoadException e) {
            messages.add(e.getMessage());
            loadFailed = true;
        }
        if (authService.ensureManager()) {
            messages.add("Manager account created: " + LoginManager.DEFAULT_MANAGER_USERNAME
                    + " / " + LoginManager.DEFAULT_MANAGER_PASSWORD
                    + ". Log in with it to create Clerk accounts.");
        }

        
        try {
            if (fileHandler.fileExists(FileHandler.PRODUCTS_FILE)) {
                // Normal run: load the saved files.
                ArrayList<Product> loaded = fileHandler.loadProducts();
                productManager.setProducts(loaded);

                if (fileHandler.fileExists(FileHandler.INVENTORY_FILE)) {
                    inventory.setItems(fileHandler.loadInventory(loaded));
                }
                // Safety net: every product must have a stock entry.
                for (Product p : loaded) {
                    if (inventory.findByProductId(p.getId()) == null) {
                        inventory.addItem(new InventoryItem(p, DEFAULT_QUANTITY, DEFAULT_REORDER_LEVEL));
                    }
                }
            } else {
                messages.add("No saved products found. Starting with an empty product list.");
            }
        } catch (DataLoadException e) {
            messages.add(e.getMessage());
            loadFailed = true;
        }

        // ---------- 3. TRANSACTIONS (the wallet balance is rebuilt from them) ----------
        try {
            if (fileHandler.fileExists(FileHandler.TRANSACTIONS_FILE)) {
                transactionManager.setTransactions(fileHandler.loadTransactions());
            }
        } catch (DataLoadException e) {
            messages.add(e.getMessage());
            loadFailed = true;
        }

        messages.addAll(fileHandler.getWarnings());

        if (loadFailed) {
            messages.add("A file could not be read, so nothing was saved on startup.");
        } else {
            try {
                saveAll();
            } catch (DataLoadException e) {
                messages.add(e.getMessage());
            }
        }
        return messages;
    }

    // Saves products, inventory, users and transactions to their files.
    // PERSISTENCE: saves products, inventory, users and transactions to the data folder. Called after every change.
    public void saveAll() throws DataLoadException {
        fileHandler.saveProducts(productManager.getProducts());
        fileHandler.saveInventory(inventory);
        fileHandler.saveUsers(authService.getUsers());
        fileHandler.saveTransactions(transactionManager.getTransactions());
    }

    // LOGIN / LOGOUT

    // Returns the logged-in User, or null if the login failed.
    // The result is really a Manager or a Clerk, but it is returned as a User (POLYMORPHISM).
    public User login(String username, String password) {
        currentUser = authService.login(username, password);
        return currentUser;
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    // ACCOUNTS

    // Manager only: creates a Clerk account for an employee to log in with.
    public void createClerkAccount(String username, String password)
            throws ValidationException, DataLoadException {
        requireAccountRights();
        authService.registerClerk(username, password);
        saveAll();
    }

    // Manager only: creates a Clerk account with a phone and location
    public void createClerkAccount(String username, String password, String phone, String location)
            throws ValidationException, DataLoadException {
        requireAccountRights();
        authService.registerClerk(username, password, phone, location);
        saveAll();
    }

    // Manager only: changes a Clerk's phone, location and (if not empty) password
    public void editClerkAccount(String username, String phone, String location, String newPassword)
            throws ValidationException, DataLoadException {
        requireAccountRights();
        authService.updateClerk(username, phone, location, newPassword);
        saveAll();
    }

    // Manager only: removes a Clerk account (their transaction history stays)
    public void removeClerkAccount(String username) throws ValidationException, DataLoadException {
        requireAccountRights();
        authService.removeClerk(username);
        saveAll();
    }

    // Manager only: returns all Clerk accounts
    public ArrayList<User> getClerkAccounts() throws ValidationException {
        requireAccountRights();
        ArrayList<User> clerks = new ArrayList<User>();
        for (User u : authService.getUsers()) {
            if (u.getRole().equals("Clerk")) {
                clerks.add(u);
            }
        }
        return clerks;
    }

    // Returns the ID the next transaction will get, for example T0004
    public String getNextTransactionId() { return transactionManager.getNextId(); }

    // PERMISSION CHECKS (private helpers)

    private void requireEditRights() throws ValidationException {
        if (currentUser == null) {
            throw new ValidationException("Please log in first.");
        }
        if (!currentUser.canEditProducts()) {
            throw new ValidationException("Access denied: only a Manager can add, edit or delete products.");
        }
    }

    private void requireRestockRights() throws ValidationException {
        if (currentUser == null) {
            throw new ValidationException("Please log in first.");
        }
        if (!currentUser.canRestock()) {
            throw new ValidationException("Access denied: only a Manager can restock or change reorder levels.");
        }
    }

    private void requireAccountRights() throws ValidationException {
        if (currentUser == null) {
            throw new ValidationException("Please log in first.");
        }
        if (!currentUser.canManageAccounts()) {
            throw new ValidationException("Access denied: only the Manager can create accounts.");
        }
    }

    private void requireTransactionRights() throws ValidationException {
        if (currentUser == null) {
            throw new ValidationException("Please log in first.");
        }
        if (!currentUser.canProcessTransactions()) {
            throw new ValidationException("Access denied: you cannot record or void transactions.");
        }
    }

    private void requireViewAllRights() throws ValidationException {
        if (currentUser == null) {
            throw new ValidationException("Please log in first.");
        }
        if (!currentUser.canViewAllTransactions()) {
            throw new ValidationException("Access denied: only a Manager can see every clerk's records.");
        }
    }

    // PRODUCT ACTIONS Manager only, each one saves afterwards
    
    public void addProduct(Product p, double initialQuantity, double reorderLevel)
            throws ValidationException, DataLoadException {
        requireEditRights();
        productManager.addProduct(p, initialQuantity, reorderLevel);
        saveAll();
    }

    public void editProduct(String id, String name, String description,
                            double price, String unit, int shelfLifeDays)
            throws ValidationException, DataLoadException {
        requireEditRights();
        productManager.editProduct(id, name, description, price, unit, shelfLifeDays);
        saveAll();
    }

    public void editEggDetails(String id, String size, int piecesPerTray)
            throws ValidationException, DataLoadException {
        requireEditRights();
        productManager.editEggDetails(id, size, piecesPerTray);
        saveAll();
    }

    public void deleteProduct(String id) throws ValidationException, DataLoadException {
        requireEditRights();
        productManager.deleteProduct(id);
        saveAll();
    }

    // INVENTORY ACTIONS (Manager only, each one saves afterwards)
    
    public void restock(String productId, double amount)
            throws ValidationException, DataLoadException {
        requireRestockRights();
        inventory.restock(productId, amount);
        saveAll();
    }

    // True if the product still has expired stock (a restock is refused until it is removed).
    public boolean hasExpiredStock(String productId) throws ValidationException {
        return inventory.hasExpiredStock(productId);
    }

    // Manager only: throws away the product's expired stock. Returns how much was removed.
    public double removeExpiredStock(String productId) throws ValidationException, DataLoadException {
        requireRestockRights();
        double removed = inventory.removeExpiredStock(productId);
        saveAll();
        return removed;
    }

    // The batches of a product (stocked date, expiry date, quantity), earliest expiry first.
    public ArrayList<StockBatch> getStockBatches(String productId) throws ValidationException {
        return inventory.getBatches(productId);
    }

    public void setReorderLevel(String productId, double level)
            throws ValidationException, DataLoadException {
        requireRestockRights();
        inventory.setReorderLevel(productId, level);
        saveAll();
    }

    // VIEWING (everyone can do these, so no permission check)
    
    public ArrayList<Product> getAllProducts() { return productManager.getProducts(); }

    public Product findProduct(String id) { return productManager.findById(id); }

    public ArrayList<Product> searchProducts(String keyword) {
        return productManager.searchByName(keyword);
    }

    public ArrayList<Product> filterProducts(String category) {
        return productManager.filterByCategory(category);
    }

    public ArrayList<InventoryItem> getInventoryItems() { return inventory.getItems(); }

    public ArrayList<InventoryItem> filterInventory(String category) {
        return inventory.filterByCategory(category);
    }

    public ArrayList<InventoryItem> getLowStockItems() { return inventory.getLowStockItems(); }

    public ArrayList<InventoryItem> getExpiringSoon(int days) {
        return inventory.getExpiringSoon(days);
    }

    // TRANSACTION ACTIONS (Manager and Clerk, each one saves afterwards)

    // Adds a product to the cart. It checks the product, the quantity and the stock,
    // counting what is already in the cart, so two small lines cannot add up to more
    // than the stock. Nothing is deducted until the sale is recorded.
    public void addToCart(Cart cart, String productId, double quantity) throws ValidationException {
        requireTransactionRights();
        if (quantity <= 0) {
            throw new ValidationException("Quantity must be greater than 0.");
        }
        transactionManager.createSaleItem(productId, cart.getQuantityOf(productId) + quantity);
        cart.addItem(transactionManager.createSaleItem(productId, quantity));
    }

    // Checks a customer name right away (not empty, no "|"), so a screen can reject it early.
    public void checkCustomerName(String customerName) throws ValidationException {
        transactionManager.checkCustomerName(customerName);
    }

    // Records the cart as a sale for a customer under the logged-in user's name, then empties the cart.
    public Sale recordSale(Cart cart, String customerName) throws ValidationException, DataLoadException {
        requireTransactionRights();
        Sale sale = transactionManager.recordSale(currentUser.getUsername(), customerName, cart.getItems());
        cart.clear();
        saveAll();
        return sale;
    }

    public void voidTransaction(String id) throws ValidationException, DataLoadException {
        requireTransactionRights();
        transactionManager.voidTransaction(id);
        saveAll();
    }

    // TRANSACTION VIEWING

    public double getWalletBalance() { return transactionManager.getWallet().getBalance(); }

    // A Manager gets every transaction, a Clerk only gets their own.
    public ArrayList<Transaction> getTransactionHistory() throws ValidationException {
        if (currentUser == null) {
            throw new ValidationException("Please log in first.");
        }
        if (currentUser.canViewAllTransactions()) {
            return transactionManager.getTransactions();
        }
        return transactionManager.getByClerk(currentUser.getUsername());
    }

    // Your visible history, narrowed to a type ("Sale") and/or a date. A blank type or null date means any.
    public ArrayList<Transaction> getTransactionHistory(String type, LocalDate date)
            throws ValidationException {
        return transactionManager.filter(getTransactionHistory(), type, date);
    }

    // Manager only: the usernames of all Clerk accounts (for a picker).
    public ArrayList<String> getClerkNames() throws ValidationException {
        requireViewAllRights();
        return new ManagerDashboard(authService, transactionManager, inventory).getClerkNames();
    }

    // Manager only: the history of one clerk, narrowed to a type and/or a date.
    public ArrayList<Transaction> getClerkHistory(String username, String type, LocalDate date)
            throws ValidationException {
        return transactionManager.filter(getClerkHistory(username), type, date);
    }

    // Manager only: the history of one clerk.
    public ArrayList<Transaction> getClerkHistory(String username) throws ValidationException {
        requireViewAllRights();
        return transactionManager.getByClerk(username);
    }

    // The logged-in user's own dashboard: a ManagerDashboard or a ClerkDashboard.
    public Dashboard getDashboard() throws ValidationException {
        if (currentUser == null) {
            throw new ValidationException("Please log in first.");
        }
        if (currentUser.canViewAllTransactions()) {
            return new ManagerDashboard(authService, transactionManager, inventory);
        }
        return new ClerkDashboard(currentUser.getUsername(), transactionManager, inventory);
    }

    // Manager only: opens the dashboard of one clerk.
    public ClerkDashboard getClerkDashboard(String username) throws ValidationException {
        requireViewAllRights();
        ManagerDashboard dashboard = new ManagerDashboard(authService, transactionManager, inventory);
        ClerkDashboard clerkDashboard = dashboard.getClerkDashboard(username);
        if (clerkDashboard == null) {
            throw new ValidationException("No clerk named " + username + ".");
        }
        return clerkDashboard;
    }
}
