package objects;

import java.util.ArrayList;

// SaleItem = one line of a sale (for example "2.5 kg of Pork Chop").
// It remembers the name and the price at the time of the sale, so changing
// the product's price later does not change old sales.
public class SaleItem {

    // ENCAPSULATION: private fields, read through getters. The price is copied at the time of the sale.
    private String productId;
    private String productName;
    private String unit;
    private double quantity;
    private double unitPrice;
    private ArrayList<StockBatch> batchesUsed;   // which batches this line took its stock from

    public SaleItem(String productId, String productName, String unit,
                    double quantity, double unitPrice) {
        this.productId = productId;
        this.productName = productName;
        this.unit = unit;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.batchesUsed = new ArrayList<StockBatch>();
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getUnit() { return unit; }
    public double getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }

    // The batches this line took its stock from (empty until the sale is recorded).
    public ArrayList<StockBatch> getBatchesUsed() { return batchesUsed; }

    public void setBatchesUsed(ArrayList<StockBatch> used) {
        batchesUsed = used;
    }

    // The batches as text for the save file: stocked/expiry/quantity separated by commas.
    public String getBatchesText() {
        String text = "";
        for (int i = 0; i < batchesUsed.size(); i++) {
            if (i > 0) {
                text = text + ",";
            }
            text = text + batchesUsed.get(i).toText();
        }
        return text;
    }

    // Used when the same product is added to a sale twice.
    public void addQuantity(double amount) {
        quantity = quantity + amount;
    }

    public double getSubtotal() {
        return quantity * unitPrice;
    }

    // One line for the transactions file: ITEM|saleId|productId|name|unit|quantity|unitPrice|batchesUsed
    public String toFileString(String saleId) {
        return "ITEM|" + saleId + "|" + productId + "|" + productName.replace("|", "/")
                + "|" + unit + "|" + quantity + "|" + unitPrice + "|" + getBatchesText();
    }
}
