package app;

import java.util.ArrayList;
import java.util.Scanner;
import dashboard.Dashboard;
import exception.DataLoadException;
import exception.ValidationException;
import objects.Beef;
import objects.Chicken;
import objects.Egg;
import objects.Cart;
import objects.InventoryItem;
import objects.Pork;
import objects.Product;
import objects.Sale;
import objects.SaleItem;
import objects.StockBatch;
import objects.Transaction;
import objects.User;

public class ConsoleApp {

    private static Scanner scanner = new Scanner(System.in);
    private static PorkingLotSystem system = new PorkingLotSystem();

        // MAIN: login loop

    public static void main(String[] args) {
        // Load saved data (or seed it) and show any messages.
        ArrayList<String> messages = system.startUp();
        for (String message : messages) {
            System.out.println("[INFO] " + message);
        }

        boolean running = true;
        while (running) {
            System.out.println("\n=== THE PORKING LOT ===");
            System.out.println(" 1  Log in");
            System.out.println(" 0  Exit");
            int choice = readInt("Choose: ");

            switch (choice) {
                case 1:  loginScreen(); break;
                case 0:  running = false; break;
                default: System.out.println("Please choose a number from the menu.");
            }
        }
        System.out.println("Goodbye!");
    }

        // LOGIN

    private static void loginScreen() {
        String username = readString("Username: ");
        String password = readString("Password: ");
        User user = system.login(username, password);
        if (user == null) {
            System.out.println("Invalid username or password.");
        } else {
            startSession(user);
        }
    }

    private static void startSession(User user) {
        System.out.println("Welcome, " + user.getUsername() + " (" + user.getRole() + ")");
        mainMenu();
    }

        // MENU

    private static void mainMenu() {
        boolean loggedIn = true;
        while (loggedIn) {
            User user = system.getCurrentUser();
            boolean canEdit = user.canEditProducts();
            boolean canStock = user.canRestock();
            boolean canProcess = user.canProcessTransactions();
            boolean canManage = user.canManageAccounts();

       
            System.out.println("\n--- MENU ---");
            System.out.println(" 1  View all products");
            System.out.println(" 2  Search products by name");
            System.out.println(" 3  Filter products by category");
            System.out.println(" 4  View inventory");
            System.out.println(" 5  View low-stock items");
            System.out.println(" 6  View items expiring soon");
            System.out.println(" 7  Filter inventory by category");

            
            System.out.println(" 8  View stock batches of a product");

            if (canProcess) {
                System.out.println(" 9  Record a sale");
                System.out.println("10  Void a transaction");
            }
            System.out.println("11  View transaction history");
            System.out.println("12  View wallet balance");
            System.out.println("13  View my dashboard");

            if (canEdit) {
                System.out.println("14  Add product");
                System.out.println("15  Edit product");
                System.out.println("16  Delete product");
            }
            if (canStock) {
                System.out.println("17  Restock product");
                System.out.println("18  Set reorder level");
            }
            if (canManage) {
                System.out.println("19  Account");
            }
            System.out.println(" 0  Logout");
            int choice = readInt("Choose: ");

            
            if ((choice >= 9 && choice <= 10 && !canProcess)
                    || (choice >= 14 && choice <= 16 && !canEdit)
                    || (choice >= 17 && choice <= 18 && !canStock)
                    || (choice == 19 && !canManage)) {
                System.out.println("Please choose a number from the menu.");
                continue;
            }

            try {
                switch (choice) {
                    case 1:  printProducts(system.getAllProducts()); break;
                    case 2:  searchProducts(); break;
                    case 3:  filterProducts(); break;
                    case 4:  printInventory(system.getInventoryItems()); break;
                    case 5:  printInventory(system.getLowStockItems()); break;
                    case 6:  viewExpiring(); break;
                    case 7:  filterInventory(); break;
                    case 8:  viewBatches(); break;
                    case 9:  recordSale(); break;
                    case 10: voidTransaction(); break;
                    case 11: printTransactions(system.getTransactionHistory()); break;
                    case 12: System.out.printf("Wallet balance: %.2f%n", system.getWalletBalance()); break;
                    case 13: printDashboard(system.getDashboard()); break;
                    case 14: addProduct(); break;
                    case 15: editProduct(); break;
                    case 16: deleteProduct(); break;
                    case 17: restock(); break;
                    case 18: setReorderLevel(); break;
                    case 19: accountMenu(); break;
                    case 0:
                        system.logout();
                        loggedIn = false;
                        System.out.println("Logged out.");
                        break;
                    default:
                        System.out.println("Please choose a number from the menu.");
                }
            } catch (ValidationException e) {
                System.out.println("[ERROR] " + e.getMessage());
            } catch (DataLoadException e) {
                System.out.println("[FILE ERROR] " + e.getMessage());
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] That was not a valid number.");
            }
        }
    }

        // VIEW ACTIONS

    private static void viewBatches() throws ValidationException {
        String id = readString("Product ID: ");
        ArrayList<StockBatch> batches = system.getStockBatches(id);
        if (batches.isEmpty()) {
            System.out.println("(no stock batches, this product has no stock)");
            return;
        }
        String unit = system.findProduct(id).getUnit();
        System.out.printf("%-12s %-12s %10s %-7s %s%n", "Stocked", "Expires", "Quantity", "Unit", "Status");
        for (StockBatch b : batches) {
            String status = "OK";
            if (b.isExpired()) {
                status = "EXPIRED";
            }
            System.out.printf("%-12s %-12s %10.2f %-7s %s%n", b.getDateStocked(), b.getExpiryDate(),
                    b.getQuantity(), unit, status);
        }
    }

    private static void searchProducts() {
        String keyword = readString("Search name: ");
        printProducts(system.searchProducts(keyword));
    }

    private static void filterProducts() {
        String category = readString("Category (Pork, Beef, Chicken, Egg): ");
        printProducts(system.filterProducts(category));
    }

    private static void filterInventory() {
        String category = readString("Category (Pork, Beef, Chicken, Egg): ");
        printInventory(system.filterInventory(category));
    }

    private static void viewExpiring() {
        int days = readInt("Show items expiring within how many days? ");
        printInventory(system.getExpiringSoon(days));
    }

        // MANAGER ACTIONS

    private static void addProduct() throws ValidationException, DataLoadException {
        // Check the category first so the manager does not type everything for nothing.
        String category = readString("Category (Pork, Beef, Chicken, Egg): ");
        if (!Product.isInList(category, Product.CATEGORIES)) {
            System.out.println("[ERROR] Category must be Pork, Beef, Chicken or Egg.");
            return;
        }

        String id = readString("Product ID: ");
        String name = readString("Name: ");
        String description = readString("Description: ");
        double price = readDouble("Price per unit: ");
        String unit = readString("Unit (kg or pieces): ");
        int shelfLife = readInt("Shelf life (days): ");

        // Build the right kind of object depending on the category.
        Product product;
        if (category.equalsIgnoreCase("Egg")) {
            String size = readString("Egg size (Small/Medium/Large/Jumbo): ");
            int perTray = readInt("Pieces per tray: ");
            product = new Egg(id, name, description, price, unit, shelfLife, size, perTray);
        } else if (category.equalsIgnoreCase("Pork")) {
            product = new Pork(id, name, description, price, unit, shelfLife);
        } else if (category.equalsIgnoreCase("Beef")) {
            product = new Beef(id, name, description, price, unit, shelfLife);
        } else {
            product = new Chicken(id, name, description, price, unit, shelfLife);
        }

        double quantity = readDouble("Initial stock quantity: ");
        double reorder = readDouble("Reorder level: ");

        system.addProduct(product, quantity, reorder);
        System.out.println("Product added.");
    }

    private static void editProduct() throws ValidationException, DataLoadException {
        String id = readString("Product ID to edit: ");
        Product p = system.findProduct(id);
        if (p == null) {
            System.out.println("[ERROR] Product not found.");
            return;
        }

        System.out.println("Editing " + p.getName() + " (press Enter to keep the current value)");
        String name = readKeep("Name", p.getName());
        String description = readKeep("Description", p.getDescription());
        double price = Double.parseDouble(readKeep("Price per unit", "" + p.getPricePerUnit()));
        String unit = readKeep("Unit", p.getUnit());
        int shelfLife = Integer.parseInt(readKeep("Shelf life (days)", "" + p.getShelfLifeDays()));
        system.editProduct(p.getId(), name, description, price, unit, shelfLife);

        // Only eggs have extra fields now.
        if (p instanceof Egg) {
            Egg egg = (Egg) p;
            String size = readKeep("Egg size", egg.getSize());
            int perTray = Integer.parseInt(readKeep("Pieces per tray", "" + egg.getPiecesPerTray()));
            system.editEggDetails(p.getId(), size, perTray);
        }
        System.out.println("Product updated.");
    }

    private static void deleteProduct() throws ValidationException, DataLoadException {
        String id = readString("Product ID to delete: ");
        if (readYesNo("Are you sure? (y/n): ")) {
            system.deleteProduct(id);
            System.out.println("Product deleted.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    private static void restock() throws ValidationException, DataLoadException {
        String id = readString("Product ID to restock: ");

        // Expired stock must be removed before new stock can be added.
        if (system.hasExpiredStock(id)) {
            System.out.println("This product has expired stock.");
            if (readYesNo("Remove the expired stock now? (y/n): ")) {
                double removed = system.removeExpiredStock(id);
                System.out.println("Expired stock removed (" + removed + "). Choose Restock again to add the new stock.");
            } else {
                System.out.println("Cannot restock because there are expired items.");
            }
            return;   // back to the menu
        }

        double amount = readDouble("Amount to add: ");
        system.restock(id, amount);
        System.out.println("Stock updated.");
    }

    private static void setReorderLevel() throws ValidationException, DataLoadException {
        String id = readString("Product ID: ");
        double level = readDouble("New reorder level: ");
        system.setReorderLevel(id, level);
        System.out.println("Reorder level updated.");
    }

        // TRANSACTION ACTIONS (Manager and Clerk)

    private static void recordSale() throws ValidationException, DataLoadException {
        String customer = readString("Customer name: ");
        system.checkCustomerName(customer);   // reject an empty name before any items are typed

        Cart cart = new Cart();
        System.out.println("Add items to the sale. Leave the Product ID empty to finish.");
        boolean adding = true;
        while (adding) {
            String id = readString("Product ID: ");
            if (id.isEmpty()) {
                adding = false;
            } else {
                double quantity = readDouble("Quantity: ");
                try {
                    system.addToCart(cart, id, quantity);
                } catch (ValidationException e) {
                    // a bad line only skips that line, the rest of the sale continues
                    System.out.println("[ERROR] " + e.getMessage());
                }
            }
        }

        if (cart.isEmpty()) {
            System.out.println("Sale cancelled (no items).");
            return;
        }

        for (SaleItem item : cart.getItems()) {
            System.out.printf("  %-24s %8.2f %-3s @ %8.2f per %-3s = %9.2f%n", item.getProductName(),
                    item.getQuantity(), item.getUnit(), item.getUnitPrice(), item.getUnit(), item.getSubtotal());
        }
        System.out.printf("  TOTAL: %.2f%n", cart.getTotal());

        if (readYesNo("Confirm sale? (y/n): ")) {
            Sale sale = system.recordSale(cart, customer);
            System.out.printf("Sale %s recorded for %s. Total: %.2f%n", sale.getId(), sale.getCustomerName(), sale.getAmount());
        } else {
            System.out.println("Sale cancelled.");
        }
    }

    private static void voidTransaction() throws ValidationException, DataLoadException {
        String id = readString("Transaction ID to void: ");
        if (readYesNo("Void " + id + "? This cannot be undone (y/n): ")) {
            system.voidTransaction(id);
            System.out.println("Transaction voided.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    // ACCOUNT MENU (Manager only)

    private static void accountMenu() {
    	boolean inAccount = true;
    	while (inAccount) {
    		System.out.println("\n--- ACCOUNT ---");
    		System.out.println(" 1  View a clerk's dashboard");
    		System.out.println(" 2  View a clerk's transaction history");
    		System.out.println(" 3  Create a clerk account");
    		System.out.println(" 0  Back");
    		int choice = readInt("Choose: ");

        try {
            switch (choice) {
                case 1:  viewClerkDashboard(); break;
                case 2:  viewClerkHistory(); break;
                case 3:  createClerkAccount(); break;
                case 0:  inAccount = false; break;
                default: System.out.println("Please choose a number from the menu.");
            }
        } catch (ValidationException e) {
            System.out.println("[ERROR] " + e.getMessage());
        } catch (DataLoadException e) {
            System.out.println("[FILE ERROR] " + e.getMessage());
        }
    }
}
    
    
        // MANAGER ACCOUNT ACTION

    private static void createClerkAccount() throws ValidationException, DataLoadException {
        String username = readString("New clerk username: ");
        String password = readString("New clerk password (at least 4 characters): ");
        system.createClerkAccount(username, password);
        System.out.println("Clerk account created. Give these login details to the clerk.");
    }

        // MANAGER VIEWS

    private static void viewClerkDashboard() throws ValidationException {
        String username = readString("Clerk username: ");
        printDashboard(system.getClerkDashboard(username));
    }

    private static void viewClerkHistory() throws ValidationException {
        String username = readString("Clerk username: ");
        printTransactions(system.getClerkHistory(username));
    }

        // PRINTING TABLES

    private static void printTransactions(ArrayList<Transaction> list) {
        if (list.isEmpty()) {
            System.out.println("(no transactions found)");
            return;
        }
        System.out.printf("%-6s %-19s %-8s %-10s %-14s %10s %-7s %s%n",
                "ID", "Date and time", "Type", "Clerk", "Customer", "Amount", "Status", "Details");
        for (Transaction t : list) {
            System.out.printf("%-6s %-19s %-8s %-10s %-14s %10.2f %-7s %s%n",
                    t.getId(), t.getDateTimeText(), t.getType(),
                    t.getClerkUsername(), t.getCustomerName(), t.getAmount(), t.getStatus(), t.getDetails());
        }
    }

    // The dashboards only give us the data, this method is the one that prints it.
    private static void printDashboard(Dashboard dashboard) {
        System.out.println("\n=== " + dashboard.getTitle() + " ===");
        for (String line : dashboard.getSummary()) {
            System.out.println(line);
        }
        System.out.println("\nRecent transactions:");
        printTransactions(dashboard.getRecentTransactions(5));
    }

    private static void printProducts(ArrayList<Product> list) {
        if (list.isEmpty()) {
            System.out.println("(no products found)");
            return;
        }
        // printf: %-6s = text, left-aligned, 6 wide.  %.2f = number with 2 decimals.
        System.out.printf("%-6s %-9s %-24s %9s %-7s %s%n",
                "ID", "Category", "Name", "Price", "Unit", "Details");
        for (Product p : list) {
            System.out.printf("%-6s %-9s %-24s %9.2f %-7s %s%n",
                    p.getId(), p.getCategory(), p.getName(),
                    p.getPricePerUnit(), p.getUnit(), p.getDetails());
        }
    }

    private static void printInventory(ArrayList<InventoryItem> list) {
        if (list.isEmpty()) {
            System.out.println("(no inventory items found)");
            return;
        }
        System.out.printf("%-6s %-24s %9s %-7s %8s %9s %-12s %s%n",
                "ID", "Name", "Stock", "Unit", "Reorder", "Expired", "Next expiry", "Status");
        for (InventoryItem item : list) {
            Product p = item.getProduct();
            System.out.printf("%-6s %-24s %9.2f %-7s %8.2f %9.2f %-12s %s%n",
                    p.getId(), p.getName(), item.getStock(), p.getUnit(),
                    item.getReorderLevel(), item.getExpiredStock(), item.getExpiryText(),
                    item.getStatus());
        }
    }

    // INPUT HELPERS: keep asking until the user gives a valid value
    
    private static String readString(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {   // input ended (for example, a test file)
            System.exit(0);
        }
        return scanner.nextLine().trim();
    }

    // Shows the current value; if the user just presses Enter, keeps it.
    private static String readKeep(String label, String current) {
        String text = readString(label + " [" + current + "]: ");
        if (text.isEmpty()) {
            return current;
        }
        return text;
    }

    private static int readInt(String prompt) {
        while (true) {
            try {
                return Integer.parseInt(readString(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            try {
                return Double.parseDouble(readString(prompt));
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    private static boolean readYesNo(String prompt) {
        String answer = readString(prompt);
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes");
    }
}