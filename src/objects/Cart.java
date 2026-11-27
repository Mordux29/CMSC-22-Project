package objects;

import java.util.ArrayList;

// Cart = the lines of a sale that is still being put together (before it is
// confirmed). It merges the same product into one line and works out the total,
// so a screen does not have to do that maths itself.
public class Cart {

    // COMPOSITION ("has-a"): a Cart HAS a list of SaleItem objects.
    private ArrayList<SaleItem> items;

    public Cart() {
        items = new ArrayList<SaleItem>();
    }

    public ArrayList<SaleItem> getItems() { return items; }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    // Adds a line. If the same product is already in the cart, its quantity is increased.
    public void addItem(SaleItem newItem) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getProductId().equalsIgnoreCase(newItem.getProductId())) {
                items.get(i).addQuantity(newItem.getQuantity());
                return;
            }
        }
        items.add(newItem);
    }

    // Takes a product's line out of the cart. Returns false if it was not in the cart.
    public boolean removeItem(String productId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getProductId().equalsIgnoreCase(productId)) {
                items.remove(i);
                return true;
            }
        }
        return false;
    }

    // How much of a product is already in the cart (0 if none).
    public double getQuantityOf(String productId) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getProductId().equalsIgnoreCase(productId)) {
                return items.get(i).getQuantity();
            }
        }
        return 0;
    }

    public double getTotal() {
        double total = 0;
        for (int i = 0; i < items.size(); i++) {
            total = total + items.get(i).getSubtotal();
        }
        return total;
    }

    // Empties the cart (after the sale is recorded or cancelled).
    public void clear() {
        items.clear();
    }
}
