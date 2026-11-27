package objects;

import java.time.LocalDate;
import java.util.ArrayList;
import exception.ValidationException;

// InventoryItem = "how much stock do we have of ONE product".
// The stock is split into BATCHES (one for each restock), and each batch has its
// own stocked and expiry date. The batches are kept in order, earliest expiry first,
// so a sale always takes the stock that expires first (FIFO).
public class InventoryItem {

    // COMPOSITION ("has-a"): an InventoryItem HAS a Product and HAS a list of StockBatch objects.
    // ENCAPSULATION: all fields are private and only changed through the methods of this class.
    private Product product;                 //an object of product
    private double reorderLevel;             // warn when the usable stock falls to this value or below
    private ArrayList<StockBatch> batches;   // earliest expiry first, empty batches are removed

    // An item with no stock yet. Used when loading batches from the save file
    // CONSTRUCTOR OVERLOADING: this class has three constructors with different parameters.
    // Java picks the right one from the values you pass in. They call each other with this(...).
    public InventoryItem(Product product, double reorderLevel) {
        this.product = product;
        this.reorderLevel = reorderLevel;
        this.batches = new ArrayList<StockBatch>();
    }

    // this constructor is for inputing a new item hence at the last constructor
    // it stores the current date, while it calls the other constructors using this
    public InventoryItem(Product product, double stock, double reorderLevel) {
        this(product, stock, reorderLevel, LocalDate.now());
    }

    // This constructor starts the item with ONE batch stocked on the given date. It is also
    // used to load the old save file format, which only had one date for the whole stock
    public InventoryItem(Product product, double stock, double reorderLevel,
                         LocalDate dateStocked) {
        this(product, reorderLevel);
        if (stock > 0) {
            addBatch(new StockBatch(stock, dateStocked, dateStocked.plusDays(product.getShelfLifeDays())));
        }
    }

    // Getters for the attributes
    public Product getProduct() { return product; }
    public double getReorderLevel() { return reorderLevel; }
    public ArrayList<StockBatch> getBatches() { return batches; }
    public int getBatchCount() { return batches.size(); }

    // The USABLE stock: the total of every batch that is not expired.
    // This is the amount that can be sold
    public double getStock() {
        double total = 0;
        for (int i = 0; i < batches.size(); i++) {
            if (!batches.get(i).isExpired()) {
                total = total + batches.get(i).getQuantity();
            }
        }
        return total;
    }

    // The total of every batch that is already expired (it cannot be sold)
    public double getExpiredStock() {
        double total = 0;
        for (int i = 0; i < batches.size(); i++) {
            if (batches.get(i).isExpired()) {
                total = total + batches.get(i).getQuantity();
            }
        }
        return total;
    }

    // Adds a batch in the right place (earliest expiry first). If a batch with the same
    // stocked and expiry dates already exists, the quantity is added to it instead
    public void addBatch(StockBatch newBatch) {
        for (int i = 0; i < batches.size(); i++) {
            if (batches.get(i).isSameBatch(newBatch.getDateStocked(), newBatch.getExpiryDate())) {
                batches.get(i).addQuantity(newBatch.getQuantity());
                return;
            }
        }
        int position = 0;
        while (position < batches.size()
                && !batches.get(position).getExpiryDate().isAfter(newBatch.getExpiryDate())) {
            position++;
        }
        batches.add(position, newBatch);
    }

    // Changes the reorder level of a product by taking in a integer
    // it would throw a exception if the user inputed a negative value
    public void setReorderLevel(double reorderLevel) throws ValidationException {
        if (reorderLevel < 0) {
            throw new ValidationException("Reorder level cannot be negative.");
        }
        this.reorderLevel = reorderLevel;
    }

    // Adds a NEW batch of stock, stocked today. It is refused while the item still has
    // expired stock, so expired meat can never be hidden by a fresh restock
    public void restock(double amount) throws ValidationException {
    	//if the amount inputed is 0 or negative, it will throw a exception an and a message
        if (amount <= 0) {
            throw new ValidationException("Restock amount must be greater than 0.");
        }
        if (hasExpiredStock()) {
            throw new ValidationException("Cannot restock " + product.getName()
                    + " because it has expired stock. Remove the expired stock first.");
        }
        LocalDate today = LocalDate.now();
        addBatch(new StockBatch(amount, today, today.plusDays(product.getShelfLifeDays())));
    }

    // Throws away every expired batch and returns how much stock was removed
    public double removeExpiredStock() {
        double removed = 0;
        for (int i = batches.size() - 1; i >= 0; i--) {
            if (batches.get(i).isExpired()) {
                removed = removed + batches.get(i).getQuantity();
                batches.remove(i);
            }
        }
        return removed;
    }

    // Deducts the amount from the batches that expire first (FIFO), skipping expired ones.
    // It returns the pieces it took from each batch, so a sale can remember them
    // and put the stock back in the same batches if the sale is voided
    public ArrayList<StockBatch> deduct(double amount) throws ValidationException {
        if (amount <= 0) {
            throw new ValidationException("Amount to deduct must be greater than 0.");
        }
        if (amount > getStock()) {
            throw new ValidationException("Not enough stock. Available: " + getStock()
                    + " " + product.getUnit() + ".");
        }
        ArrayList<StockBatch> used = new ArrayList<StockBatch>();
        double remaining = amount;
        int i = 0;
        while (remaining > 0.000001 && i < batches.size()) {
            StockBatch batch = batches.get(i);
            if (batch.isExpired()) {
                i++;
            } else {
                double taken = Math.min(remaining, batch.getQuantity());
                used.add(new StockBatch(taken, batch.getDateStocked(), batch.getExpiryDate()));
                batch.removeQuantity(taken);
                remaining = remaining - taken;
                if (batch.getQuantity() <= 0.000001) {
                    batches.remove(i);   // an empty batch is removed, so we do not move on
                } else {
                    i++;
                }
            }
        }
        return used;
    }

    // Puts stock back into the SAME batches it was taken from (used when a sale is voided).
    // If a batch is gone, it is created again with its original dates
    public void returnStock(ArrayList<StockBatch> used) {
        for (int i = 0; i < used.size(); i++) {
            StockBatch piece = used.get(i);
            addBatch(new StockBatch(piece.getQuantity(), piece.getDateStocked(), piece.getExpiryDate()));
        }
    }

    // Only for old sales that did not remember their batches: the stock goes back into
    // the batch that expires first, or into a new batch of today if there is none
    public void returnStock(double amount) throws ValidationException {
        if (amount <= 0) {
            throw new ValidationException("Amount to return must be greater than 0.");
        }
        if (batches.isEmpty()) {
            LocalDate today = LocalDate.now();
            addBatch(new StockBatch(amount, today, today.plusDays(product.getShelfLifeDays())));
        } else {
            batches.get(0).addQuantity(amount);
        }
    }

    //Checker if the usable stock is low and needs to be reordered
    //Will return True if the usable stock is below or equal to the reorderLevel of a product
    public boolean isLowStock() {
        return getStock() <= reorderLevel;
    }

    // True when at least one batch is expired
    public boolean hasExpiredStock() {
        return getExpiredStock() > 0;
    }

    // Returns true if the item has expired stock
    public boolean isExpired() {
        return hasExpiredStock();
    }

    // The expiry date of the batch that expires first, or null if there is no stock at all
    public LocalDate getExpiryDate() {
        if (batches.isEmpty()) {
            return null;
        }
        return batches.get(0).getExpiryDate();
    }

    // The same date as text for tables, "-" when there is no stock
    public String getExpiryText() {
        if (batches.isEmpty()) {
            return "-";
        }
        return batches.get(0).getExpiryDate().toString();
    }

    // True when a batch expires within the next "days" days.
    // Items that are already expired also count (their expiry is before today).
    public boolean isExpiringSoon(int days) {
        if (batches.isEmpty()) {
            return false;
        }
        LocalDate limit = LocalDate.now().plusDays(days);
        return !batches.get(0).getExpiryDate().isAfter(limit);
    }

    // The status text shown in tables: "EXPIRED", "LOW-STOCK", both, or "OK".
    public String getStatus() {
        String status = "";
        if (isExpired()) {
            status = status + "EXPIRED ";
        }
        if (isLowStock()) {
            status = status + "LOW-STOCK";
        }
        if (status.isEmpty()) {
            status = "OK";
        }
        return status.trim();
    }

    // The lines for the inventory save file: one INV line, then one BATCH line for each batch.
    // INV|productId|reorderLevel
    // BATCH|productId|quantity|dateStocked|expiryDate
    public String toFileString() {
        String text = "INV|" + product.getId() + "|" + reorderLevel;
        for (int i = 0; i < batches.size(); i++) {
            StockBatch b = batches.get(i);
            text = text + "\nBATCH|" + product.getId() + "|" + b.getQuantity() + "|"
                    + b.getDateStocked() + "|" + b.getExpiryDate();
        }
        return text;
    }
}
