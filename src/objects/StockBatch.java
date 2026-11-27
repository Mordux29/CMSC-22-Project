package objects;

import java.time.LocalDate;

// StockBatch = one delivery of a product. Every restock creates its own batch with
// its own quantity, the date it was stocked and its own expiry date, so the
// shop always knows which stock expires first.
// It is also used to remember which batches a sale took its stock from.
public class StockBatch {

    private double quantity;        // in kg or pieces (depends on the product's unit)
    private LocalDate dateStocked;
    private LocalDate expiryDate;   // fixed when the batch is created (stocked date + shelf life)

    public StockBatch(double quantity, LocalDate dateStocked, LocalDate expiryDate) {
        this.quantity = quantity;
        this.dateStocked = dateStocked;
        this.expiryDate = expiryDate;
    }

    public double getQuantity() { return quantity; }
    public LocalDate getDateStocked() { return dateStocked; }
    public LocalDate getExpiryDate() { return expiryDate; }

    public void addQuantity(double amount) {
        quantity = quantity + amount;
    }

    public void removeQuantity(double amount) {
        quantity = quantity - amount;
    }

    // True when today is already past the expiry date.
    public boolean isExpired() {
        return LocalDate.now().isAfter(expiryDate);
    }

    // Two batches are the same batch when they were stocked and expire on the same dates.
    public boolean isSameBatch(LocalDate stocked, LocalDate expiry) {
        return dateStocked.equals(stocked) && expiryDate.equals(expiry);
    }

    // Text used in the save files: stockedDate/expiryDate/quantity
    // PERSISTENCE: turns the batch into text for the save file, and fromText() below turns it back into an object.
    public String toText() {
        return dateStocked + "/" + expiryDate + "/" + quantity;
    }

    // Rebuilds a batch from the text written by toText(). Throws an exception if it is damaged.
    // "static": called as StockBatch.fromText(...), no object needed. It builds and returns a new StockBatch.
    public static StockBatch fromText(String text) {
        String[] parts = text.split("/");
        if (parts.length != 3) {
            throw new IllegalArgumentException("a batch needs 3 values but found " + parts.length);
        }
        return new StockBatch(Double.parseDouble(parts[2]), LocalDate.parse(parts[0]),
                LocalDate.parse(parts[1]));
    }
}
