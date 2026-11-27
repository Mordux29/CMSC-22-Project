package managers;

import java.util.ArrayList;
import exception.ValidationException;
import objects.InventoryItem;
import objects.StockBatch;

// Inventory it holds ALL InventoryItems and offers
// methods to find, restock, filter and remove them
// ENCAPSULATION: the list of items is private. Other classes must use the methods here (find, restock...),
// so the rules (like "amount must be more than 0") are always checked in one place.
public class InventoryManager {

    private ArrayList<InventoryItem> items;

    public InventoryManager() {
        items = new ArrayList<InventoryItem>();
    }

    public ArrayList<InventoryItem> getItems() { return items; }

    // Replaces the whole list, used when loading from a file.
    public void setItems(ArrayList<InventoryItem> items) { this.items = items; }

    public void addItem(InventoryItem item) {
        items.add(item);
    }

    // Finds the stock entry of a product. Returns null if there is none.
    public InventoryItem findByProductId(String productId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getProduct().getId().equalsIgnoreCase(productId)) {
                return items.get(i);
            }
        }
        return null;
    }

    // Adds stock to a product.
    public void restock(String productId, double amount) throws ValidationException {
        InventoryItem item = findByProductId(productId);
        if (item == null) {
            throw new ValidationException("No inventory entry for product ID " + productId + ".");
        }
        item.restock(amount); // the item itself checks that amount > 0
    }

    // True if the product has stock that is already expired.
    public boolean hasExpiredStock(String productId) throws ValidationException {
        return findOrFail(productId).hasExpiredStock();
    }

    // Throws away the expired stock of a product and returns how much was removed.
    public double removeExpiredStock(String productId) throws ValidationException {
        return findOrFail(productId).removeExpiredStock();
    }

    // The batches of a product, earliest expiry first.
    public ArrayList<StockBatch> getBatches(String productId) throws ValidationException {
        return findOrFail(productId).getBatches();
    }

    private InventoryItem findOrFail(String productId) throws ValidationException {
        InventoryItem item = findByProductId(productId);
        if (item == null) {
            throw new ValidationException("No inventory entry for product ID " + productId + ".");
        }
        return item;
    }

    // Changes the low-stock threshold of a product.
    public void setReorderLevel(String productId, double level) throws ValidationException {
        InventoryItem item = findByProductId(productId);
        if (item == null) {
            throw new ValidationException("No inventory entry for product ID " + productId + ".");
        }
        item.setReorderLevel(level);
    }

    // Removes the stock entry of a product (used when the product is deleted).
    public void removeItem(String productId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getProduct().getId().equalsIgnoreCase(productId)) {
                items.remove(i);
                return; // we found it, so we can stop looping
            }
        }
    }

    // All items whose quantity is at or below their reorder level.
    public ArrayList<InventoryItem> getLowStockItems() {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isLowStock()) {
                result.add(items.get(i));
            }
        }
        return result;
    }

    // All items that expire within "days" days (already expired ones included).
    public ArrayList<InventoryItem> getExpiringSoon(int days) {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isExpiringSoon(days)) {
                result.add(items.get(i));
            }
        }
        return result;
    }

    // All items whose product belongs to the given category ("Pork", "Egg", ...).
    public ArrayList<InventoryItem> filterByCategory(String category) {
        ArrayList<InventoryItem> result = new ArrayList<InventoryItem>();
        for (int i = 0; i < items.size(); i++) {
            // getCategory() is POLYMORPHIC: each product class answers for itself
            if (items.get(i).getProduct().getCategory().equalsIgnoreCase(category)) {
                result.add(items.get(i));
            }
        }
        return result;
    }
}
