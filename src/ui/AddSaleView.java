package ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import app.PorkingLotSystem;
import exception.DataLoadException;
import exception.ValidationException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import objects.Cart;
import objects.InventoryItem;
import objects.Product;
import objects.SaleItem;

// Add Sale screen: product picker on the left, the cart on the right
// INHERITANCE: extends HBox, so this screen IS a horizontal box with the picker on the left and the cart on the right.
public class AddSaleView extends HBox {
    // One line of the product table (a product, its stock, and a tick box)
    public static class ProductRow {
        Product product;
        InventoryItem stock;
        BooleanProperty checked = new SimpleBooleanProperty(false);

        // Stores the product, its inventory item and an unticked box
        ProductRow(Product product, InventoryItem stock) {
            this.product = product;
            this.stock = stock;
        }
    }

    private PorkingLotSystem system;
    private Runnable onDone;
    private Runnable onSold;

    private Cart cart = new Cart();
    private String customerName = "";
    private boolean customerSaved;
    private int quantity = 1;
    private String category = "All";
    private String searchText = "";

    private ObservableList<ProductRow> allRows = FXCollections.observableArrayList();
    private FilteredList<ProductRow> shownRows = new FilteredList<ProductRow>(allRows, r -> true);
    private TableView<ProductRow> table = new TableView<ProductRow>();

    private Label customerLabel = new Label();
    private VBox cartRows = new VBox();
    private Label totalLabel = new Label();

    // Builds the screen; onDone runs after a sale is confirmed
    public AddSaleView(PorkingLotSystem system, Runnable onDone) {
        this(system, onDone, onDone);
    }

    // Same screen, but with a separate action for the Return button and for a confirmed sale
    public AddSaleView(PorkingLotSystem system, Runnable onDone, Runnable onSold) {
        this.system = system;
        this.onDone = onDone;
        this.onSold = onSold;
        setSpacing(14);

        VBox left = buildLeft();
        VBox right = buildRight();
        HBox.setHgrow(left, Priority.ALWAYS);
        getChildren().addAll(left, right);
        getStyleClass().add("card");
        setPadding(new Insets(12));

        loadProducts();
        refreshCart();
    }

    // Builds the left side: search box, category filter, product table, quantity and Add to Cart
    private VBox buildLeft() {
        TextField search = new TextField();
        search.setPromptText("Search");
        search.textProperty().addListener((obs, oldText, newText) -> {
            searchText = newText.trim().toLowerCase();
            applyFilter();
        });
        HBox searchBox = new HBox(8, Icons.outline(Icons.SEARCH, 18), search);
        searchBox.getStyleClass().add("search-box");
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPrefHeight(38);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        MenuButton categoryButton = new MenuButton("Category");
        categoryButton.getStyleClass().add("tool-button");
        categoryButton.getItems().addAll(Dialogs.radioItems(
                new String[] { "All", "Pork", "Beef", "Chicken", "Egg" },
                choice -> {
                    category = choice;
                    applyFilter();
                }));

        HBox top = new HBox(10, searchBox, categoryButton);
        top.setAlignment(Pos.CENTER_LEFT);

        buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        Label quantityLabel = new Label("1");
        quantityLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        quantityLabel.setMinWidth(30);
        quantityLabel.setAlignment(Pos.CENTER);
        Button minus = new Button("-");
        Button plus = new Button("+");
        minus.getStyleClass().add("qty-button");
        plus.getStyleClass().add("qty-button");
        minus.setOnAction(e -> {
            if (quantity > 1) {
                quantity--;
                quantityLabel.setText(String.valueOf(quantity));
            }
        });
        plus.setOnAction(e -> {
            quantity++;
            quantityLabel.setText(String.valueOf(quantity));
        });
        Button addToCart = new Button("Add to Cart");
        addToCart.getStyleClass().add("tool-button");
        addToCart.setPrefWidth(160);
        addToCart.setOnAction(e -> addCheckedToCart());

        Region spacer = Dialogs.spacer();
        HBox bottom = new HBox(10, spacer, minus, quantityLabel, plus, addToCart);
        bottom.setAlignment(Pos.CENTER_RIGHT);

        VBox left = new VBox(12, top, table, bottom);
        left.setPadding(new Insets(0, 6, 0, 0));
        return left;
    }

    // Builds the product table with its tick box column
    private void buildTable() {
        TableColumn<ProductRow, Boolean> check = new TableColumn<ProductRow, Boolean>();
        check.setCellValueFactory(cell -> cell.getValue().checked);
        check.setCellFactory(CheckBoxTableCell.forTableColumn(check));
        check.setEditable(true);
        check.setSortable(false);
        check.setMaxWidth(44);
        check.setMinWidth(44);
        CheckBox all = new CheckBox();
        all.setOnAction(e -> {
            for (ProductRow r : shownRows) {
                r.checked.set(all.isSelected());
            }
        });
        check.setGraphic(all);
        table.getColumns().add(check);

        addColumn("ID", r -> r.product.getId());
        addColumn("NAME", r -> r.product.getName());
        addColumn("UNIT PRICE", r -> Format.PESO + Format.price(r.product.getPricePerUnit()) + " / " + r.product.getUnit());
        addColumn("AVAILABLE STOCK", r -> Format.price(r.stock == null ? 0 : r.stock.getStock()) + " " + r.product.getUnit());
        addColumn("STATUS", r -> statusOf(r));

        table.setEditable(true);
        table.setItems(shownRows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No products found."));
    }

    // Gives the text a column shows for one product row
    private interface CellText {
        String get(ProductRow r);
    }

    // Adds one text column to the product table
    private void addColumn(String title, CellText text) {
        TableColumn<ProductRow, String> column = new TableColumn<ProductRow, String>(title);
        column.setCellValueFactory(cell -> new SimpleStringProperty(text.get(cell.getValue())));
        column.setSortable(false);
        table.getColumns().add(column);
    }

    // Gives the stock status text of a product: In stock, Low stock, Out of stock or Expired
    private String statusOf(ProductRow r) {
        if (r.stock == null || StockRules.isOut(r.stock)) {
            if (r.stock != null && r.stock.hasExpiredStock()) {
                return "Expired";
            }
            return "Out of stock";
        }
        if (StockRules.isLow(r.stock)) {
            return "Low stock";
        }
        return "In stock";
    }

    // Loads every product together with its inventory item into the table
    private void loadProducts() {
        Map<String, InventoryItem> stockById = new HashMap<String, InventoryItem>();
        for (InventoryItem item : system.getInventoryItems()) {
            stockById.put(item.getProduct().getId(), item);
        }
        allRows.clear();
        for (Product p : system.getAllProducts()) {
            allRows.add(new ProductRow(p, stockById.get(p.getId())));
        }
    }

    // Shows only the products that match the category and the search text (name or ID)
    private void applyFilter() {
        shownRows.setPredicate(r ->
                (category.equals("All") || r.product.getCategory().equalsIgnoreCase(category))
                && (r.product.getName().toLowerCase().contains(searchText)
                    || r.product.getId().toLowerCase().contains(searchText)));
    }

    // Adds every ticked product to the cart using the quantity chosen at the bottom
    private void addCheckedToCart() {
        ArrayList<ProductRow> checked = new ArrayList<ProductRow>();
        for (ProductRow r : allRows) {
            if (r.checked.get()) {
                checked.add(r);
            }
        }
        if (checked.isEmpty()) {
            Dialogs.error("Tick a product first before adding to the cart.");
            return;
        }
        try {
            for (ProductRow r : checked) {
                system.addToCart(cart, r.product.getId(), quantity);
                r.checked.set(false);
            }
        } catch (ValidationException e) {
            Dialogs.error(e.getMessage());
        }
        refreshCart();
    }

    // Builds the right side: transaction ID, customer, cart table, total and the two buttons
    private VBox buildRight() {
        Label id = new Label("Transaction ID: " + system.getNextTransactionId());
        id.getStyleClass().add("cart-title");
        Region gap = Dialogs.spacer();
        HBox idRow = new HBox(id, gap, Icons.outline(Icons.CART, 22));
        idRow.setAlignment(Pos.CENTER_LEFT);

        customerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        Button newCustomer = new Button("New Customer", Icons.outline(Icons.USER_ADD, 14));
        newCustomer.getStyleClass().add("tool-button");
        newCustomer.setStyle("-fx-font-size: 11px; -fx-pref-height: 26;");
        newCustomer.setOnAction(e -> askCustomerName());
        Region gap2 = Dialogs.spacer();
        HBox customerRow = new HBox(customerLabel, gap2, newCustomer);
        customerRow.setAlignment(Pos.CENTER_LEFT);

        VBox cartCard = new VBox(cartLine(null, "ITEM NAME", "UNIT PRICE", null, "SUBTOTAL", true), scrollOf(cartRows));
        cartCard.setStyle("-fx-background-color: white; -fx-background-radius: 6;");
        VBox.setVgrow(cartCard, Priority.ALWAYS);

        Label totalText = new Label("TOTAL:");
        totalText.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        totalLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Region gap3 = Dialogs.spacer();
        HBox totalRow = new HBox(totalText, gap3, totalLabel);

        Button clear = new Button("Clear Cart", Icons.outline(Icons.CART, 14));
        clear.getStyleClass().add("tool-button");
        clear.setContentDisplay(ContentDisplay.LEFT);
        clear.setOnAction(e -> {
            cart.clear();
            refreshCart();
        });
        Button confirm = new Button("Confirm Sale", Icons.outline(Icons.RETURN, 14));
        confirm.setGraphic(Icons.outline("M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0 M8 12l3 3 5-6", 16));
        confirm.getStyleClass().add("tool-button");
        confirm.setOnAction(e -> confirmSale());
        clear.setPrefWidth(150);
        confirm.setPrefWidth(150);
        HBox buttons = new HBox(12, clear, confirm);
        buttons.setAlignment(Pos.CENTER);

        VBox totalCard = new VBox(10, totalRow, buttons);
        totalCard.setPadding(new Insets(10));
        totalCard.setStyle("-fx-background-color: white; -fx-background-radius: 6;");

        VBox right = new VBox(10, idRow, customerRow, cartCard, totalCard);
        right.getStyleClass().add("cart-panel");
        right.setPadding(new Insets(14));
        right.setPrefWidth(430);
        right.setMinWidth(430);
        right.setMaxWidth(430);
        return right;
    }

    // Puts a box of rows inside a scroll pane
    private ScrollPane scrollOf(VBox box) {
        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    // Builds one line of the cart table (or the header line of it)
    private HBox cartLine(Node left, String name, String price, Node quantityCell, String subtotal, boolean header) {
        Node first = left == null ? new Region() : left;
        StackPane firstBox = new StackPane(first);
        firstBox.setMinWidth(26);
        firstBox.setPrefWidth(26);

        Label n = new Label(name);
        n.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(n, Priority.ALWAYS);
        Label p = new Label(price);
        p.setMinWidth(70);
        Node q = quantityCell;
        if (q == null) {
            q = new Label(header ? "QUANTITY" : "");
        }
        HBox qBox = new HBox(q);
        qBox.setMinWidth(84);
        qBox.setPrefWidth(84);
        qBox.setAlignment(Pos.CENTER);
        Label s = new Label(subtotal);
        s.setMinWidth(74);
        s.setAlignment(Pos.CENTER_RIGHT);

        HBox line = new HBox(4, firstBox, n, p, qBox, s);
        line.setAlignment(Pos.CENTER_LEFT);
        line.setPadding(new Insets(header ? 6 : 8, 6, header ? 6 : 8, 6));
        line.setStyle((header ? "-fx-font-size: 9px; -fx-background-color: #F0F0F0;" : "-fx-font-size: 11px;")
                + "-fx-border-color: #CFCFCF; -fx-border-width: 0 0 1 0;");
        return line;
    }

    // Rebuilds the cart lines, the total and the customer text from the Cart object
    private void refreshCart() {
        cartRows.getChildren().clear();
        for (SaleItem item : cart.getItems()) {
            Button remove = new Button();
            remove.setGraphic(removeIcon());
            remove.getStyleClass().add("cart-remove");
            String id = item.getProductId();
            remove.setOnAction(e -> {
                cart.removeItem(id);
                refreshCart();
            });

            Button minus = new Button("-");
            Button plus = new Button("+");
            minus.setStyle("-fx-background-color: transparent; -fx-padding: 0 4 0 4; -fx-cursor: hand;");
            plus.setStyle("-fx-background-color: transparent; -fx-padding: 0 4 0 4; -fx-cursor: hand;");
            minus.setOnAction(e -> changeQuantity(item, -1));
            plus.setOnAction(e -> changeQuantity(item, 1));
            Label q = new Label(Format.price(item.getQuantity()));
            q.setStyle("-fx-font-weight: bold;");
            HBox quantityCell = new HBox(4, minus, q, plus);
            quantityCell.setAlignment(Pos.CENTER);

            cartRows.getChildren().add(cartLine(remove, item.getProductName(),
                    Format.PESO + Format.price(item.getUnitPrice()) + " / " + item.getUnit(), quantityCell,
                    Format.money(item.getSubtotal()), false));
        }
        totalLabel.setText(Format.money(cart.getTotal()));
        customerLabel.setText("Customer: " + (customerName.isEmpty() ? "-" : customerName));
    }

    // Draws the small red circle with a minus used to remove a cart line
    private Node removeIcon() {
        Circle circle = new Circle(8, Color.TRANSPARENT);
        circle.setStroke(Color.web("#D32F2F"));
        Line line = new Line(-4, 0, 4, 0);
        line.setStroke(Color.web("#D32F2F"));
        return new StackPane(circle, line);
    }

    // Handles the + and - on a cart line (the line keeps its place in the cart)
    private void changeQuantity(SaleItem item, double change) {
        String id = item.getProductId();
        double oldQuantity = item.getQuantity();
        double newQuantity = oldQuantity + change;
        int position = cart.getItems().indexOf(item);
        cart.removeItem(id);
        try {
            if (newQuantity > 0) {
                system.addToCart(cart, id, newQuantity);
            }
        } catch (ValidationException e) {
            Dialogs.error(e.getMessage());
            try {
                system.addToCart(cart, id, oldQuantity);
            } catch (ValidationException ignored) {
            }
        }
        moveLastTo(position);
        refreshCart();
    }

    // Moves the newest cart line back to the given position
    private void moveLastTo(int position) {
        ArrayList<SaleItem> items = cart.getItems();
        if (position >= 0 && position < items.size() - 1) {
            SaleItem last = items.remove(items.size() - 1);
            items.add(position, last);
        }
    }

    // Opens the customer pop-up; returns true if a valid name was saved
    private boolean askCustomerName() {
        TextField name = new TextField(customerName);
        Label title = new Label("CUSTOMER");
        title.getStyleClass().add("dialog-title");
        Button save = Dialogs.pillButton("Save");
        Button cancel = Dialogs.pillButton("Cancel");
        Region fill = Dialogs.spacer();
        VBox form = new VBox(10, title, Dialogs.row("Customer name:", name), new HBox(10, fill, save, cancel));
        form.setPrefWidth(380);

        customerSaved = false;
        Stage stage = Dialogs.popup(table, form);
        cancel.setOnAction(e -> stage.close());
        save.setOnAction(e -> {
            try {
                system.checkCustomerName(name.getText());
                customerName = name.getText().trim();
                customerSaved = true;
                stage.close();
            } catch (ValidationException ex) {
                Dialogs.error(ex.getMessage());
            }
        });
        stage.showAndWait();
        refreshCart();
        return customerSaved;
    }

    // Records the sale (asks for the customer name first if it is empty)
    private void confirmSale() {
        if (cart.isEmpty()) {
            Dialogs.error("The cart is empty. Add a product first.");
            return;
        }
        if (customerName.isEmpty() && !askCustomerName()) {
            return;
        }
        try {
            system.recordSale(cart, customerName);
            onSold.run();
        } catch (ValidationException e) {
            Dialogs.error(e.getMessage());
        } catch (DataLoadException e) {
            Dialogs.error(e.getMessage());
        }
    }
}
