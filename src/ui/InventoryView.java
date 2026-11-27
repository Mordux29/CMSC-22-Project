package ui;

import java.time.LocalDate;
import app.PorkingLotSystem;
import dashboard.Dashboard;
import exception.DataLoadException;
import exception.ValidationException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import objects.InventoryItem;
import objects.StockBatch;

// Inventory page: stock counters, a table of stock batches, Restock and Set Reorder
// INHERITANCE: this page extends VBox, so it IS a VBox (a vertical box) and can hold other nodes.
// It also implements Searchable, so it must provide the search() method.
public class InventoryView extends VBox implements Searchable {
    // What a Save button does with the typed number
    private interface AmountAction {
        void run(double amount) throws ValidationException, DataLoadException;
    }

    // One table row: one stock batch of a product (or just the product if it has no batches)
    public static class Row {
        InventoryItem item;
        StockBatch batch;

        // Stores the inventory item and the batch
        Row(InventoryItem item, StockBatch batch) {
            this.item = item;
            this.batch = batch;
        }
    }

    private PorkingLotSystem system;
    private TableView<Row> table = new TableView<Row>();
    private ObservableList<Row> rows = FXCollections.observableArrayList();

    private Label lowValue = new Label();
    private Label outValue = new Label();
    private Label expiringValue = new Label();
    private Label expiredValue = new Label();

    private String searchText = "";
    private String category = "All";
    private String status = "All";

    // Builds the toolbar, the four counter cards and the table
    public InventoryView(PorkingLotSystem system) {
        this.system = system;
        setSpacing(12);

        MenuButton filter = new MenuButton("Filter", Icons.outline(Icons.FILTER, 18));
        filter.getStyleClass().add("tool-button");
        Menu categoryMenu = new Menu("Category");
        categoryMenu.getItems().addAll(Dialogs.radioItems(
                new String[] { "All", "Pork", "Beef", "Chicken", "Egg" }, c -> { category = c; refresh(); }));
        Menu statusMenu = new Menu("Status");
        statusMenu.getItems().addAll(Dialogs.radioItems(
                new String[] { "All", "Low stock", "Expiring soon", "Expired", "Out of stock" },
                s -> { status = s; refresh(); }));
        filter.getItems().addAll(categoryMenu, statusMenu);

        Region spacer = Dialogs.spacer();
        HBox toolbar = new HBox(10, filter, spacer);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        if (system.getCurrentUser().canRestock()) {
            Button restock = Dialogs.toolButton("Restock", Icons.BOX);
            restock.setOnAction(e -> restockSelected());
            Button reorder = Dialogs.toolButton("Set Reorder", Icons.REORDER);
            reorder.setOnAction(e -> setReorderSelected());
            toolbar.getChildren().addAll(restock, reorder);
        }

        HBox counters = new HBox(24,
                counterCard("Low Stock", Icons.BOX, lowValue),
                counterCard("Out of Stock", Icons.EMPTY, outValue),
                counterCard("Expiring", Icons.CLOCK, expiringValue),
                counterCard("Expired", Icons.ALERT, expiredValue));

        buildTable();
        VBox tableCard = new VBox(table);
        tableCard.getStyleClass().add("card");
        tableCard.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        getChildren().addAll(toolbar, counters, tableCard);
        refresh();
    }

    // Builds one counter card, for example "Low Stock - 4 Items"
    private VBox counterCard(String title, String icon, Label value) {
        Label label = new Label(title);
        label.getStyleClass().add("mini-card-label");
        HBox top = new HBox(8, Icons.outline(icon, 18), label);
        top.setAlignment(Pos.CENTER_LEFT);
        value.getStyleClass().add("mini-card-value");
        VBox card = new VBox(6, top, value);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(12, 16, 12, 16));
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPrefWidth(100);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    // Builds the inventory table and its columns
    private void buildTable() {
        addColumn("ID", r -> r.item.getProduct().getId());
        addColumn("NAME", r -> r.item.getProduct().getName());
        addColumn("STOCK", r -> Format.stock(r.batch == null ? 0 : r.batch.getQuantity()) + " " + r.item.getProduct().getUnit());
        addColumn("REORDER LEVEL", r -> Format.stock(r.item.getReorderLevel()) + " " + r.item.getProduct().getUnit());
        addColumn("DATE STOCKED", r -> r.batch == null ? "-" : r.batch.getDateStocked().toString());
        addColumn("EXPIRY", r -> r.batch == null ? "-" : r.batch.getExpiryDate().toString());
        addColumn("STATUS", r -> statusOf(r));
        table.setItems(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No inventory items found."));
    }

    // Gives the text a column shows for one table row
    private interface CellText {
        String get(Row r);
    }

    // Adds one text column to the table
    private void addColumn(String title, CellText text) {
        TableColumn<Row, String> column = new TableColumn<Row, String>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(text.get(cell.getValue())));
        column.setSortable(false);
        table.getColumns().add(column);
    }

    // Gives the status text of one row: Expired, Out of stock, Low stock, Expiring or OK
    private String statusOf(Row r) {
        if (r.batch != null && r.batch.isExpired()) {
            return "Expired";
        }
        if (StockRules.isOut(r.item)) {
            return "Out of stock";
        }
        if (StockRules.isLow(r.item)) {
            return "Low stock";
        }
        if (r.batch != null && !r.batch.getExpiryDate().isAfter(LocalDate.now().plusDays(Dashboard.EXPIRING_DAYS))) {
            return "Expiring";
        }
        return "OK";
    }

    // Called by the header search box: keeps the text and reloads the table
    // @Override: this is the method promised by the Searchable interface (POLYMORPHISM).
    @Override
    public void search(String text) {
        searchText = text.trim().toLowerCase();
        refresh();
    }

    // Updates the four counters and reloads the table rows (using the filter and search)
    public void refresh() {
        int low = 0, out = 0, expiring = 0, expired = 0;
        rows.clear();
        for (InventoryItem item : system.getInventoryItems()) {
            if (StockRules.isLow(item)) low++;
            if (StockRules.isOut(item)) out++;
            if (StockRules.isExpiring(item)) expiring++;
            if (StockRules.isExpired(item)) expired++;

            if (matches(item)) {
                if (item.getBatches().isEmpty()) {
                    rows.add(new Row(item, null));
                }
                for (StockBatch b : item.getBatches()) {
                    rows.add(new Row(item, b));
                }
            }
        }
        lowValue.setText(items(low));
        outValue.setText(items(out));
        expiringValue.setText(items(expiring));
        expiredValue.setText(items(expired));
    }

    // Checks if an item passes the category filter, the status filter and the search (name or ID)
    private boolean matches(InventoryItem item) {
        boolean categoryOk = category.equals("All") || item.getProduct().getCategory().equalsIgnoreCase(category);
        boolean searchOk = item.getProduct().getName().toLowerCase().contains(searchText)
                || item.getProduct().getId().toLowerCase().contains(searchText);
        boolean statusOk = status.equals("All")
                || (status.equals("Low stock") && StockRules.isLow(item))
                || (status.equals("Expiring soon") && StockRules.isExpiring(item))
                || (status.equals("Expired") && StockRules.isExpired(item))
                || (status.equals("Out of stock") && StockRules.isOut(item));
        return categoryOk && searchOk && statusOk;
    }

    // Returns the item of the selected table row (shows a message if no row is selected)
    private InventoryItem selectedItem() {
        Row row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            Dialogs.info("Select a row in the table first.");
            return null;
        }
        return row.item;
    }

    // Restocks the selected item (expired stock has to be removed first)
    private void restockSelected() {
        InventoryItem item = selectedItem();
        if (item == null) {
            return;
        }
        String id = item.getProduct().getId();
        String unit = item.getProduct().getUnit();
        try {
            if (system.hasExpiredStock(id)) {
                if (Dialogs.confirm(item.getProduct().getName() + " has expired stock. Remove it now?\n"
                        + "Then press Restock again to add the new stock.")) {
                    system.removeExpiredStock(id);
                    refresh();
                }
                return;
            }
        } catch (ValidationException e) {
            Dialogs.error(e.getMessage());
            return;
        } catch (DataLoadException e) {
            Dialogs.error(e.getMessage());
            return;
        }
        showAmountDialog(item, "Restock", "Current Stock:", Format.stock(item.getStock()), "Amount to Add:", unit,
                amount -> system.restock(id, amount));
    }

    // Changes the reorder level of the selected item
    private void setReorderSelected() {
        InventoryItem item = selectedItem();
        if (item == null) {
            return;
        }
        String id = item.getProduct().getId();
        showAmountDialog(item, "Set Reorder Level", "Current Reorder Level:", Format.stock(item.getReorderLevel()),
                "New Reorder Level:", item.getProduct().getUnit(), level -> system.setReorderLevel(id, level));
    }

    // Shows the small Restock / Set Reorder pop-up and runs the given action on Save
    private void showAmountDialog(InventoryItem item, String action, String currentLabel, String current,
                                  String newLabel, String unit, AmountAction onSave) {
        Label name = new Label(item.getProduct().getName().toUpperCase());
        name.getStyleClass().add("dialog-title");
        Label actionLabel = new Label(action);
        Region gap = Dialogs.spacer();
        HBox titleRow = new HBox(name, gap, actionLabel);

        TextField currentField = new TextField(current);
        currentField.setEditable(false);
        TextField newField = new TextField();

        Button save = Dialogs.pillButton("Save");
        Button cancel = Dialogs.pillButton("Cancel");
        Region fill = Dialogs.spacer();
        HBox buttons = new HBox(10, fill, save, cancel);

        VBox form = new VBox(10, titleRow, Dialogs.row(currentLabel, withUnit(currentField, unit)),
                Dialogs.row(newLabel, withUnit(newField, unit)), buttons);
        form.setPrefWidth(400);

        Stage stage = Dialogs.popup(table, form);
        cancel.setOnAction(e -> stage.close());
        save.setOnAction(e -> {
            try {
                onSave.run(Double.parseDouble(newField.getText().trim()));
                stage.close();
                refresh();
            } catch (NumberFormatException ex) {
                Dialogs.error("Please enter a valid number.");
            } catch (ValidationException ex) {
                Dialogs.error(ex.getMessage());
            } catch (DataLoadException ex) {
                Dialogs.error(ex.getMessage());
            }
        });
        stage.showAndWait();
    }

    // Puts the unit (kg, pieces) at the right end inside a text field
    private StackPane withUnit(TextField field, String unit) {
        Label unitLabel = new Label(unit);
        unitLabel.setMouseTransparent(true);
        unitLabel.setStyle("-fx-font-size: 12px;");
        StackPane.setAlignment(unitLabel, Pos.CENTER_RIGHT);
        StackPane.setMargin(unitLabel, new Insets(0, 12, 0, 0));
        field.setStyle("-fx-padding: 5 56 5 8;");
        return new StackPane(field, unitLabel);
    }

    // Writes a count as "1 Item" or "4 Items"
    private String items(int count) {
        return count + (count == 1 ? " Item" : " Items");
    }
}
