package managers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import exception.ValidationException;
import objects.InventoryItem;
import objects.Product;
import objects.Sale;
import objects.SaleItem;
import objects.Transaction;
import objects.Wallet;

// TransactionManager records sales, voids them, keeps the
// wallet balance, and answers history questions (by clerk, date, type).
// It works together with InventoryManager (to deduct and return stock) and
// ProductManager (to find products and their prices).
// This manager's job: transactions and the wallet. It works together with InventoryManager and ProductManager
// (COMPOSITION: it keeps references to them), instead of copying their code.
public class TransactionManager {

    private ArrayList<Transaction> transactions;
    private Wallet wallet;
    private InventoryManager inventory;
    private ProductManager productManager;

    public TransactionManager(InventoryManager inventory, ProductManager productManager) {
        this.inventory = inventory;
        this.productManager = productManager;
        this.transactions = new ArrayList<Transaction>();
        this.wallet = new Wallet();
    }

    public ArrayList<Transaction> getTransactions() { return transactions; }
    public Wallet getWallet() { return wallet; }

    // Replaces the whole list (used when loading from a file) and rebuilds the wallet.
    public void setTransactions(ArrayList<Transaction> transactions) {
        this.transactions = transactions;
        rebuildWallet();
    }

    // The wallet is not saved on its own. Its balance is always the total of
    // every transaction that is not voided, so it can never disagree with the history.
    private void rebuildWallet() {
        wallet = new Wallet();
        for (int i = 0; i < transactions.size(); i++) {
            Transaction t = transactions.get(i);
            if (!t.isVoided() && t.getAmount() > 0) {
                try {
                    wallet.deposit(t.getAmount());
                } catch (ValidationException e) {
                    // cannot happen: we only deposit amounts greater than 0
                }
            }
        }
    }

    // RECORDING

    // Builds ONE line of a sale. Checks that the product exists, the quantity is
    // greater than 0 and there is enough usable (not expired) stock.
    public SaleItem createSaleItem(String productId, double quantity) throws ValidationException {
        Product p = productManager.findById(productId);
        if (p == null) {
            throw new ValidationException("Product ID " + productId + " was not found.");
        }
        if (quantity <= 0) {
            throw new ValidationException("Quantity must be greater than 0.");
        }
        checkStock(inventory.findByProductId(p.getId()), p.getName(), p.getUnit(), quantity);
        return new SaleItem(p.getId(), p.getName(), p.getUnit(), quantity, p.getPricePerUnit());
    }

    // Checks that the item exists and that this quantity can be sold from its usable stock.
    private void checkStock(InventoryItem stock, String name, String unit, double quantity)
            throws ValidationException {
        if (stock == null) {
            throw new ValidationException("No inventory entry for " + name + ".");
        }
        if (quantity > stock.getStock()) {
            if (stock.getStock() <= 0 && stock.hasExpiredStock()) {
                throw new ValidationException(name + " is expired and cannot be sold.");
            }
            throw new ValidationException("Not enough stock of " + name + ". Available: "
                    + stock.getStock() + " " + unit + " (you asked for " + quantity + " " + unit + ").");
        }
    }

    // The customer name cannot be empty and cannot contain "|" (our file separator).
    public void checkCustomerName(String customerName) throws ValidationException {
        checkText(customerName, "Customer name");
    }

    // Records a sale: checks all the stock first, then deducts it (earliest expiry first),
    // adds the total to the wallet and saves the sale in the history. Each line remembers
    // the batches it took its stock from, so a void can put it back in the same batches.
    // Returns the recorded Sale.
    public Sale recordSale(String clerkUsername, String customerName, ArrayList<SaleItem> cart)
            throws ValidationException {
        checkCustomerName(customerName);
        if (cart.isEmpty()) {
            throw new ValidationException("A sale needs at least one item.");
        }

        Sale sale = new Sale(nextId(), LocalDateTime.now().withNano(0), clerkUsername, false,
                customerName.trim());
        for (int i = 0; i < cart.size(); i++) {
            SaleItem line = cart.get(i);
            // new copy, so the cart the caller passed in is not changed when lines merge
            sale.addItem(new SaleItem(line.getProductId(), line.getProductName(), line.getUnit(),
                    line.getQuantity(), line.getUnitPrice()));
        }

        // Check EVERYTHING before changing anything, so a failed sale changes nothing.
        for (int i = 0; i < sale.getItems().size(); i++) {
            SaleItem line = sale.getItems().get(i);
            checkStock(inventory.findByProductId(line.getProductId()), line.getProductName(),
                    line.getUnit(), line.getQuantity());
        }

        for (int i = 0; i < sale.getItems().size(); i++) {
            SaleItem line = sale.getItems().get(i);
            line.setBatchesUsed(inventory.findByProductId(line.getProductId()).deduct(line.getQuantity()));
        }
        wallet.deposit(sale.getAmount());
        transactions.add(sale);
        return sale;
    }

    // VOIDING

    // Voids a transaction. The record stays in the history, marked as voided.
    // A voided sale returns its stock and its money is taken out of the wallet.
    public void voidTransaction(String id) throws ValidationException {
        Transaction t = findById(id);
        if (t == null) {
            throw new ValidationException("Transaction " + id + " was not found.");
        }
        if (t.isVoided()) {
            throw new ValidationException("Transaction " + t.getId() + " is already voided.");
        }

        // Money first: if the wallet cannot cover it, nothing has changed yet.
        if (t.getAmount() > 0) {
            wallet.withdraw(t.getAmount());
        }

        // A sale puts its items back in stock (a deleted product has no entry, so skip it).
        if (t instanceof Sale) {
            Sale sale = (Sale) t;
            for (int i = 0; i < sale.getItems().size(); i++) {
                SaleItem line = sale.getItems().get(i);
                InventoryItem stock = inventory.findByProductId(line.getProductId());
                if (stock != null) {
                    if (line.getBatchesUsed().isEmpty()) {
                        stock.returnStock(line.getQuantity());      // an old sale that has no batch record
                    } else {
                        stock.returnStock(line.getBatchesUsed());   // back into the same batches
                    }
                }
            }
        }
        t.markVoided();
    }

    // HISTORY

    public Transaction findById(String id) {
        for (int i = 0; i < transactions.size(); i++) {
            if (transactions.get(i).getId().equalsIgnoreCase(id.trim())) {
                return transactions.get(i);
            }
        }
        return null;
    }

    public ArrayList<Transaction> getByClerk(String username) {
        ArrayList<Transaction> result = new ArrayList<Transaction>();
        for (int i = 0; i < transactions.size(); i++) {
            if (transactions.get(i).getClerkUsername().equalsIgnoreCase(username.trim())) {
                result.add(transactions.get(i));
            }
        }
        return result;
    }

    public ArrayList<Transaction> getByDate(LocalDate date) {
        ArrayList<Transaction> result = new ArrayList<Transaction>();
        for (int i = 0; i < transactions.size(); i++) {
            if (transactions.get(i).getDateTime().toLocalDate().equals(date)) {
                result.add(transactions.get(i));
            }
        }
        return result;
    }

    // All transactions of one type, for example "Sale".
    public ArrayList<Transaction> getByType(String type) {
        ArrayList<Transaction> result = new ArrayList<Transaction>();
        for (int i = 0; i < transactions.size(); i++) {
            if (transactions.get(i).getType().equalsIgnoreCase(type)) {
                result.add(transactions.get(i));
            }
        }
        return result;
    }

    // Only the transactions that count (voided ones are left out).
    public ArrayList<Transaction> getActive(ArrayList<Transaction> list) {
        ArrayList<Transaction> result = new ArrayList<Transaction>();
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).isVoided()) {
                result.add(list.get(i));
            }
        }
        return result;
    }

    // Keeps only the transactions of a type and a date. A blank type or a null
    // date means "any", so filter(list, "", null) returns everything.
    public ArrayList<Transaction> filter(ArrayList<Transaction> list, String type, LocalDate date) {
        ArrayList<Transaction> result = new ArrayList<Transaction>();
        for (int i = 0; i < list.size(); i++) {
            Transaction t = list.get(i);
            boolean typeMatches = type == null || type.trim().isEmpty()
                    || t.getType().equalsIgnoreCase(type.trim());
            boolean dateMatches = date == null || t.getDateTime().toLocalDate().equals(date);
            if (typeMatches && dateMatches) {
                result.add(t);
            }
        }
        return result;
    }

    // The sales of one date that count (voided ones are left out).
    public ArrayList<Transaction> getActiveSalesOn(ArrayList<Transaction> list, LocalDate date) {
        return getActive(filter(list, "Sale", date));
    }

    // Adds up the money of the transactions in the list, skipping voided ones.
    public double sumAmounts(ArrayList<Transaction> list) {
        double total = 0;
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).isVoided()) {
                total = total + list.get(i).getAmount();
            }
        }
        return total;
    }

    // PRIVATE HELPERS

    // Returns the ID the next transaction will get (to show it before the sale is confirmed)
    public String getNextId() {
        return nextId();
    }

    // T0001, T0002, ... one more than the highest number used so far (so an ID is never reused).
    private String nextId() {
        int highest = 0;
        for (int i = 0; i < transactions.size(); i++) {
            String id = transactions.get(i).getId();
            try {
                int number = Integer.parseInt(id.substring(1));
                if (number > highest) {
                    highest = number;
                }
            } catch (NumberFormatException e) {
                // an ID that is not in the T0000 format is ignored
            }
        }
        String text = "" + (highest + 1);
        while (text.length() < 4) {
            text = "0" + text;
        }
        return "T" + text;
    }

    // Text that is saved in the file cannot be empty and cannot contain "|".
    private void checkText(String text, String label) throws ValidationException {
        if (text == null || text.trim().isEmpty()) {
            throw new ValidationException(label + " cannot be empty.");
        }
        if (text.contains("|")) {
            throw new ValidationException(label + " cannot contain the \"|\" character.");
        }
    }
}
