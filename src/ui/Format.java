package ui;

// Number and money text shared by the pages, so every page shows them the same way
public class Format {
    public static final String PESO = "\u20B1";

    // Shows an amount as money, for example 1250 becomes ₱1,250.00
    public static String money(double amount) {
        return PESO + String.format("%,.2f", amount);
    }

    // Shows a price or quantity, for example 365.0 becomes 365 and 12.5 becomes 12.50
    public static String price(double value) {
        if (value == Math.floor(value)) {
            return String.format("%,.0f", value);
        }
        return String.format("%,.2f", value);
    }

    // Shows a stock amount, for example 8.0 becomes 8 and 2.5 becomes 2.5
    public static String stock(double value) {
        if (value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%.1f", value);
    }
}
