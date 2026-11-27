package persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import exception.DataLoadException;
import managers.InventoryManager;
import objects.Beef;
import objects.Chicken;
import objects.Clerk;
import objects.Egg;
import objects.InventoryItem;
import objects.Manager;
import objects.Pork;
import objects.Product;
import objects.Sale;
import objects.StockBatch;
import objects.SaleItem;
import objects.Transaction;
import objects.User;


// PERSISTENCE means keeping the data after the program is closed. Objects only live in memory while the
// program runs, so this class SAVES them as lines of text in the data folder, and LOADS them back into
// objects the next time the program starts. It is the only class that reads or writes files, so the rest of
// the program does not need to know how the files look (ABSTRACTION).
public class FileHandler {

    // Relative paths: they work on every computer when you run from Eclipse,
    // as long as the "data" folder sits directly inside the project folder.
    public static final String DATA_FOLDER = "data";
    public static final String PRODUCTS_FILE = "data/products.txt";
    public static final String INVENTORY_FILE = "data/inventory.txt";
    public static final String USERS_FILE = "data/users.txt";
    public static final String TRANSACTIONS_FILE = "data/transactions.txt";

    // Number of fields per line, so we do not repeat "magic numbers" below.
    private static final int MEAT_FIELDS = 7;
    private static final int EGG_FIELDS = 9;

    // Messages about skipped lines. The screen/console can show them to the user.
    private ArrayList<String> warnings;

    public FileHandler() {
        warnings = new ArrayList<String>();
    }

    public ArrayList<String> getWarnings() { return warnings; }

    // True if the file exists.
    public boolean fileExists(String path) {
        return new File(path).exists();
    }

    // ------------------------------------------------------------------
    // PRODUCTS
    // ------------------------------------------------------------------

    // SAVE: turns every Product object into a line of text (using toFileString) and writes the file.
    public void saveProducts(ArrayList<Product> products) throws DataLoadException {
        ArrayList<String> lines = new ArrayList<String>();
        for (Product p : products) {            // "for each product p in the list"
            lines.add(p.toFileString());        // polymorphism: each class writes itself
        }
        writeLines(PRODUCTS_FILE, lines);
    }

    // LOAD: reads the file line by line and rebuilds a Product object from each line.
    public ArrayList<Product> loadProducts() throws DataLoadException {
        ArrayList<Product> result = new ArrayList<Product>();
        ArrayList<String> lines = readLines(PRODUCTS_FILE);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;   // ignore blank lines
            }
            try {
                result.add(parseProduct(line));
            } catch (Exception e) {
                // A bad number, a missing field, an unknown category... skip this line.
                warnings.add("products.txt line " + (i + 1) + " skipped: " + e.getMessage());
            }
        }
        return result;
    }

    // Turns ONE text line back into a Product object of the right class.
    // POLYMORPHISM: the category in the line decides which child class to create (Pork, Beef, Chicken or Egg),
    // but we store the result as a Product.
    private Product parseProduct(String line) {
        // "-1" keeps empty fields at the end instead of dropping them.
        // "\\|" is needed because "|" has a special meaning in split().
        String[] parts = line.split("\\|", -1);
        String category = parts[0];

        // The expected number of fields depends on the category.
        int expected;
        if (category.equals("Egg")) {
            expected = EGG_FIELDS;
        } else if (category.equals("Pork") || category.equals("Beef") || category.equals("Chicken")) {
            expected = MEAT_FIELDS;
        } else {
            throw new IllegalArgumentException("unknown category \"" + category + "\"");
        }
        if (parts.length != expected) {
            throw new IllegalArgumentException("expected " + expected + " fields but found " + parts.length);
        }

        String id = parts[1];
        String name = parts[2];
        String description = parts[3];
        double price = Double.parseDouble(parts[4]);    // may throw NumberFormatException
        String unit = parts[5];
        int shelfLife = Integer.parseInt(parts[6]);

        if (category.equals("Pork")) {
            return new Pork(id, name, description, price, unit, shelfLife);
        } else if (category.equals("Beef")) {
            return new Beef(id, name, description, price, unit, shelfLife);
        } else if (category.equals("Chicken")) {
            return new Chicken(id, name, description, price, unit, shelfLife);
        } else {
            return new Egg(id, name, description, price, unit, shelfLife,
                    parts[7], Integer.parseInt(parts[8]));
        }
    }

    // ------------------------------------------------------------------
    // INVENTORY
    // ------------------------------------------------------------------

    public void saveInventory(InventoryManager inventory) throws DataLoadException {
        ArrayList<String> lines = new ArrayList<String>();
        for (InventoryItem item : inventory.getItems()) {
            lines.add(item.toFileString());
        }
        writeLines(INVENTORY_FILE, lines);
    }

    // Loads the inventory file. An INV line starts an item and the BATCH lines after it
    // add its batches. Each line only stores the product ID, so we need the already-loaded
    // product list to find the matching Product object.
    // The OLD format (productId|quantity|reorderLevel|dateStocked, with no INV or BATCH word)
    // is still understood: it becomes an item with one batch.
    public ArrayList<InventoryItem> loadInventory(ArrayList<Product> products)
            throws DataLoadException {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        ArrayList<String> lines = readLines(INVENTORY_FILE);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                String[] parts = line.split("\\|", -1);
                if (parts[0].equals("INV")) {
                    if (parts.length != 3) {
                        throw new IllegalArgumentException("expected 3 fields but found " + parts.length);
                    }
                    result.add(new InventoryItem(findExistingProduct(products, parts[1]),
                            Double.parseDouble(parts[2])));
                } else if (parts[0].equals("BATCH")) {
                    if (parts.length != 5) {
                        throw new IllegalArgumentException("expected 5 fields but found " + parts.length);
                    }
                    InventoryItem item = findItem(result, parts[1]);
                    if (item == null) {
                        throw new IllegalArgumentException("no inventory item for product ID " + parts[1]);
                    }
                    double quantity = Double.parseDouble(parts[2]);
                    if (quantity > 0) {
                        item.addBatch(new StockBatch(quantity, LocalDate.parse(parts[3]),
                                LocalDate.parse(parts[4])));
                    }
                } else {
                    // old format
                    if (parts.length != 4) {
                        throw new IllegalArgumentException("expected 4 fields but found " + parts.length);
                    }
                    result.add(new InventoryItem(findExistingProduct(products, parts[0]),
                            Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                            LocalDate.parse(parts[3])));
                }
            } catch (Exception e) {
                warnings.add("inventory.txt line " + (i + 1) + " skipped: " + e.getMessage());
            }
        }
        return result;
    }

    private Product findExistingProduct(ArrayList<Product> products, String id) {
        Product product = findProduct(products, id);
        if (product == null) {
            throw new IllegalArgumentException("no product with ID " + id);
        }
        return product;
    }

    private InventoryItem findItem(ArrayList<InventoryItem> items, String productId) {
        for (InventoryItem item : items) {
            if (item.getProduct().getId().equalsIgnoreCase(productId)) {
                return item;
            }
        }
        return null;
    }

    private Product findProduct(ArrayList<Product> products, String id) {
        for (Product p : products) {
            if (p.getId().equalsIgnoreCase(id)) {
                return p;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // USERS
    // ------------------------------------------------------------------

    public void saveUsers(ArrayList<User> users) throws DataLoadException {
        ArrayList<String> lines = new ArrayList<String>();
        for (User u : users) {
            lines.add(u.toFileString());
        }
        writeLines(USERS_FILE, lines);
    }

    public ArrayList<User> loadUsers() throws DataLoadException {
        ArrayList<User> result = new ArrayList<User>();
        ArrayList<String> lines = readLines(USERS_FILE);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                String[] parts = line.split("\\|", -1);
                if (parts.length != 3 && parts.length != 5) {
                    throw new IllegalArgumentException("expected 3 or 5 fields but found " + parts.length);
                }
                User user;
                if (parts[2].equals("Manager")) {
                    user = new Manager(parts[0], parts[1]);
                } else if (parts[2].equals("Clerk")) {
                    user = new Clerk(parts[0], parts[1]);
                } else {
                    throw new IllegalArgumentException("unknown role \"" + parts[2] + "\"");
                }
                if (parts.length == 5) {   // phone and location (older files do not have them)
                    user.setPhone(parts[3]);
                    user.setLocation(parts[4]);
                }
                result.add(user);
            } catch (Exception e) {
                warnings.add("users.txt line " + (i + 1) + " skipped: " + e.getMessage());
            }
        }
        return result;
    }

    // ------------------------------------------------------------------
    // TRANSACTIONS
    //   SALE|id|dateTime|clerk|voided|customer
    //   ITEM|saleId|productId|name|unit|quantity|unitPrice|batchesUsed   (one line per product sold)
    // ------------------------------------------------------------------

    public void saveTransactions(ArrayList<Transaction> transactions) throws DataLoadException {
        ArrayList<String> lines = new ArrayList<String>();
        for (Transaction t : transactions) {
            lines.add(t.toFileString());   // polymorphism: a Sale also writes its ITEM lines
        }
        writeLines(TRANSACTIONS_FILE, lines);
    }

    public ArrayList<Transaction> loadTransactions() throws DataLoadException {
        ArrayList<Transaction> result = new ArrayList<Transaction>();
        ArrayList<String> lines = readLines(TRANSACTIONS_FILE);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                String[] parts = line.split("\\|", -1);
                if (parts[0].equals("SALE")) {
                    // 6 fields now (with the customer name); the old format had 5 and no customer
                    if (parts.length != 6 && parts.length != 5) {
                        throw new IllegalArgumentException("expected 6 fields but found " + parts.length);
                    }
                    String customer = "Walk-in";
                    if (parts.length == 6) {
                        customer = parts[5];
                    }
                    result.add(new Sale(parts[1], LocalDateTime.parse(parts[2]), parts[3],
                            Boolean.parseBoolean(parts[4]), customer));
                } else if (parts[0].equals("ITEM")) {
                    // 8 fields now (with the batches used); the old format had 7
                    if (parts.length != 8 && parts.length != 7) {
                        throw new IllegalArgumentException("expected 8 fields but found " + parts.length);
                    }
                    Sale sale = findSale(result, parts[1]);
                    if (sale == null) {
                        throw new IllegalArgumentException("no sale with ID " + parts[1]);
                    }
                    SaleItem item = new SaleItem(parts[2], parts[3], parts[4],
                            Double.parseDouble(parts[5]), Double.parseDouble(parts[6]));
                    if (parts.length == 8 && !parts[7].isEmpty()) {
                        for (String text : parts[7].split(",")) {
                            item.getBatchesUsed().add(StockBatch.fromText(text));
                        }
                    }
                    sale.addItem(item);
                } else {
                    throw new IllegalArgumentException("unknown line type \"" + parts[0] + "\"");
                }
            } catch (Exception e) {
                warnings.add("transactions.txt line " + (i + 1) + " skipped: " + e.getMessage());
            }
        }
        return result;
    }

    private Sale findSale(ArrayList<Transaction> list, String id) {
        for (Transaction t : list) {
            if (t instanceof Sale && t.getId().equalsIgnoreCase(id)) {
                return (Sale) t;
            }
        }
        return null;
    }

        // LOW-LEVEL HELPERS: read every line / write every line of a file
    
    // Reads all lines of a text file.
    // "try (...)" is "try-with-resources": Java closes the file for us
    // automatically, even if an error happens.
    private ArrayList<String> readLines(String path) throws DataLoadException {
        ArrayList<String> lines = new ArrayList<String>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new DataLoadException("Could not read " + path + ": " + e.getMessage());
        }
        return lines;
    }

    // Writes the lines to a file (replacing the old content).
    private void writeLines(String path, ArrayList<String> lines) throws DataLoadException {
        new File(DATA_FOLDER).mkdirs();   // make sure the "data" folder exists
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            for (String line : lines) {
                writer.println(line);
            }
        } catch (IOException e) {
            throw new DataLoadException("Could not save " + path + ": " + e.getMessage());
        }
    }
}