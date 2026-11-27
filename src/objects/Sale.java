package objects;

import java.time.LocalDateTime;
import java.util.ArrayList;

// Sale = one customer purchase. It holds a list of SaleItems and its total.
// INHERITANCE: Sale extends Transaction, so it already has an ID, date, clerk and voided flag.
// COMPOSITION ("has-a"): a Sale HAS a list of SaleItem objects.
public class Sale extends Transaction {

    private ArrayList<SaleItem> items;

    public Sale(String id, LocalDateTime dateTime, String clerkUsername, boolean voided,
                String customerName) {
        // super(...) fills in the shared fields using the parent (Transaction) constructor.
        super(id, dateTime, clerkUsername, voided, customerName);
        items = new ArrayList<SaleItem>();
    }

    public ArrayList<SaleItem> getItems() { return items; }

    // Adds a line. If the same product is already in the sale, its quantity is increased.
    public void addItem(SaleItem newItem) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getProductId().equalsIgnoreCase(newItem.getProductId())) {
                items.get(i).addQuantity(newItem.getQuantity());
                return;
            }
        }
        items.add(newItem);
    }

    // @Override: Sale's own version of the abstract methods declared in Transaction (POLYMORPHISM).
    @Override
    public String getType() {
        return "Sale";
    }

    // The total of all lines.
    @Override
    public double getAmount() {
        double total = 0;
        for (int i = 0; i < items.size(); i++) {
            total = total + items.get(i).getSubtotal();
        }
        return total;
    }

    @Override
    public String getDetails() {
        String text = "";
        for (int i = 0; i < items.size(); i++) {
            SaleItem item = items.get(i);
            if (i > 0) {
                text = text + "; ";
            }
            text = text + item.getProductName() + " x" + item.getQuantity() + " " + item.getUnit();
        }
        return text;
    }

    // The SALE line first, then one ITEM line for each product sold.
    // PERSISTENCE: the lines written to transactions.txt to save this sale.
    @Override
    public String toFileString() {
        String text = "SALE|" + baseFileString();
        for (int i = 0; i < items.size(); i++) {
            text = text + "\n" + items.get(i).toFileString(getId());
        }
        return text;
    }
}
