package ui;

import java.util.ArrayList;
import java.util.function.Consumer;
import app.PorkingLotSystem;
import exception.DataLoadException;
import exception.ValidationException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import objects.Transaction;
import objects.User;

// Accounts page (Manager only): shows the list of Clerk accounts with Add, Edit and Remove
// INHERITANCE: this page extends VBox, so it IS a VBox (a vertical box) and can hold other nodes.
// It also implements Searchable, so it must provide the search() method.
public class AccountsView extends VBox implements Searchable {
    private PorkingLotSystem system;
    private Consumer<String> onViewHistory;
    private VBox list = new VBox(8);
    private String searchText = "";

    private User selected;
    private VBox selectedCard;

    // Builds the toolbar buttons and the scrolling list of account cards
    public AccountsView(PorkingLotSystem system, Consumer<String> onViewHistory) {
        this.system = system;
        this.onViewHistory = onViewHistory;
        setSpacing(14);

        Button add = Dialogs.toolButton("Add Account", Icons.ADD);
        add.setOnAction(e -> showAddDialog());
        Button remove = Dialogs.toolButton("Remove Account", Icons.DELETE);
        remove.setOnAction(e -> removeSelected());
        Button edit = Dialogs.toolButton("Edit Account", Icons.EDIT);
        edit.setOnAction(e -> showEditDialog());
        Region spacer = Dialogs.spacer();
        HBox toolbar = new HBox(10, add, remove, spacer, edit);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        list.setPadding(new Insets(4));
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(toolbar, scroll);
        refresh();
    }

    // Called by the header search box: keeps the text and reloads the list
    // @Override: this is the method promised by the Searchable interface (POLYMORPHISM).
    @Override
    public void search(String text) {
        searchText = text.trim().toLowerCase();
        refresh();
    }

    // Reloads the account cards (only the ones that match the search text)
    public void refresh() {
        selected = null;
        selectedCard = null;
        list.getChildren().clear();
        ArrayList<User> clerks;
        try {
            clerks = system.getClerkAccounts();
        } catch (ValidationException e) {
            return;
        }
        for (User u : clerks) {
            boolean match = u.getUsername().toLowerCase().contains(searchText)
                    || u.getPhone().toLowerCase().contains(searchText)
                    || u.getLocation().toLowerCase().contains(searchText);
            if (match) {
                list.getChildren().add(makeCard(u));
            }
        }
        if (list.getChildren().isEmpty()) {
            Label none = new Label("No accounts found.");
            none.getStyleClass().add("empty-text");
            list.getChildren().add(none);
        }
    }

    // Builds one account card: name, role, total sales, phone, location and the View Sale History button
    private VBox makeCard(User u) {
        Label name = new Label(u.getUsername());
        name.getStyleClass().add("account-name");
        Label role = new Label(u.getRole());
        role.getStyleClass().add("account-role");

        HBox details = new HBox(60, detail("Total Sales", Format.PESO + String.format("%,.2f", totalSalesOf(u))),
                detail("Phone", u.getPhone().isEmpty() ? "-" : u.getPhone()),
                detail("Location", u.getLocation().isEmpty() ? "-" : u.getLocation()));

        Button history = new Button("View Sale History");
        history.getStyleClass().add("tool-button");
        history.setOnAction(e -> onViewHistory.accept(u.getUsername()));

        VBox texts = new VBox(2, name, role, new Region(), details);
        Region spacer = Dialogs.spacer();
        HBox top = new HBox(texts, spacer, history);
        top.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(texts, Priority.ALWAYS);

        VBox card = new VBox(top);
        card.getStyleClass().add("account-card");
        card.setOnMouseClicked(e -> {
            if (selectedCard != null) {
                selectedCard.getStyleClass().remove("card-selected");
            }
            selected = u;
            selectedCard = card;
            card.getStyleClass().add("card-selected");
        });
        return card;
    }

    // Builds a small label pair, for example "Phone" above "0995..."
    private VBox detail(String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("account-key");
        Label v = new Label(value);
        v.getStyleClass().add("account-value");
        VBox box = new VBox(2, k, v);
        box.setMinWidth(150);
        return box;
    }

    // Adds up the money of the clerk's sales that were not voided
    private double totalSalesOf(User u) {
        double total = 0;
        try {
            for (Transaction t : system.getClerkHistory(u.getUsername())) {
                if (!t.isVoided()) {
                    total += t.getAmount();
                }
            }
        } catch (ValidationException e) {
        }
        return total;
    }

    // Removes the clicked account after asking for confirmation (the sale history stays)
    private void removeSelected() {
        if (selected == null) {
            Dialogs.error("Click an account first before removing.");
            return;
        }
        if (Dialogs.confirm("Remove " + selected.getUsername() + "? Their sale history will stay.")) {
            try {
                system.removeClerkAccount(selected.getUsername());
                refresh();
            } catch (ValidationException e) {
                Dialogs.error(e.getMessage());
            } catch (DataLoadException e) {
                Dialogs.error(e.getMessage());
            }
        }
    }

    // Opens the Add Account pop-up and creates the clerk account when Create is pressed
    private void showAddDialog() {
        TextField username = new TextField();
        TextField phone = new TextField();
        digitsOnly(phone);
        TextField location = new TextField();
        PasswordField password = new PasswordField();
        PasswordField confirm = new PasswordField();

        Button create = Dialogs.pillButton("Create");
        Button cancel = Dialogs.pillButton("Cancel");
        VBox fields = new VBox(10, Dialogs.row("Username:", username), Dialogs.row("Phone:", phone),
                Dialogs.row("Location:", location), Dialogs.row("Password:", password),
                Dialogs.row("Confirm Password:", confirm));
        Stage stage = form("Add Account", create, cancel, fields);
        cancel.setOnAction(e -> stage.close());
        create.setOnAction(e -> {
            if (!password.getText().equals(confirm.getText())) {
                Dialogs.error("The passwords do not match.");
                return;
            }
            try {
                system.createClerkAccount(username.getText(), password.getText(), phone.getText(), location.getText());
                stage.close();
                refresh();
            } catch (ValidationException ex) {
                Dialogs.error(ex.getMessage());
            } catch (DataLoadException ex) {
                Dialogs.error(ex.getMessage());
            }
        });
        stage.showAndWait();
    }

    // Opens the Edit Account pop-up and saves the new phone, location and optional password
    private void showEditDialog() {
        if (selected == null) {
            Dialogs.error("Click an account first before editing.");
            return;
        }

        TextField username = new TextField(selected.getUsername());
        username.setDisable(true);
        TextField phone = new TextField(selected.getPhone());
        digitsOnly(phone);
        TextField location = new TextField(selected.getLocation());
        PasswordField password = new PasswordField();
        password.setPromptText("(Optional)");
        PasswordField confirm = new PasswordField();

        Button save = Dialogs.pillButton("Save");
        Button cancel = Dialogs.pillButton("Cancel");
        VBox fields = new VBox(10, Dialogs.row("Username:", username), Dialogs.row("Phone:", phone),
                Dialogs.row("Location:", location), Dialogs.row("New Password:", password),
                Dialogs.row("Confirm Password:", confirm));
        Stage stage = form("Edit Account", save, cancel, fields);
        cancel.setOnAction(e -> stage.close());
        save.setOnAction(e -> {
            if (!password.getText().equals(confirm.getText())) {
                Dialogs.error("The passwords do not match.");
                return;
            }
            try {
                system.editClerkAccount(selected.getUsername(), phone.getText(), location.getText(), password.getText());
                stage.close();
                refresh();
            } catch (ValidationException ex) {
                Dialogs.error(ex.getMessage());
            } catch (DataLoadException ex) {
                Dialogs.error(ex.getMessage());
            }
        });
        stage.showAndWait();
    }

    // Makes a phone field accept only digits, +, - and spaces (letters are removed as they are typed)
    private void digitsOnly(TextField field) {
        field.textProperty().addListener((obs, oldText, newText) -> {
            field.setText(newText.replaceAll("[^0-9+\\- ]", ""));
        });
    }

    // Builds a pop-up with a title, the given form fields, and the two buttons at the bottom right
    private Stage form(String title, Button ok, Button cancel, VBox fields) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-title");
        Region fill = Dialogs.spacer();
        VBox box = new VBox(10, titleLabel, fields, new HBox(10, fill, ok, cancel));
        box.setPrefWidth(400);
        return Dialogs.popup(list, box);
    }
}
