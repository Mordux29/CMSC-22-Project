package ui;

import java.util.ArrayList;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

// Small helpers shared by the pages: pop-ups, alerts, form rows, filter menus and common buttons
// The methods here are "static": they belong to the class, so they are called as Dialogs.error(...) without creating an object.
public class Dialogs {
    private static boolean answeredYes;

    // Creates a modal pop-up window that shows the given content
    public static Stage popup(Node anyNodeOnScreen, Node content) {
        content.getStyleClass().add("dialog-card");
        return popupOn(anyNodeOnScreen.getScene().getWindow(), content);
    }

    // Finds the window the user is working in (alerts have no node to start from)
    public static Window activeWindow() {
        for (Window w : Window.getWindows()) {
            if (w.isShowing() && w.isFocused()) {
                return w;
            }
        }
        for (Window w : Window.getWindows()) {
            if (w.isShowing()) {
                return w;
            }
        }
        return null;
    }

    // Creates the transparent, rounded pop-up window on top of the given window
    private static Stage popupOn(Window owner, Node content) {
        StackPane root = new StackPane(content);
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);
        return stage;
    }

    // Creates an empty space that pushes the next items in a row to the far side
    public static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    // Creates the white outlined toolbar button with an icon
    public static Button toolButton(String text, String iconPath) {
        Button button = new Button(text, Icons.outline(iconPath, 18));
        button.getStyleClass().add("tool-button");
        button.setContentDisplay(ContentDisplay.LEFT);
        button.setGraphicTextGap(8);
        return button;
    }

    // Creates the small pink rounded button used in pop-ups (Save, Cancel...)
    public static Button pillButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("pill-button");
        return button;
    }

    // Creates one line of a form: a label and its field
    public static HBox row(String label, Node field) {
        Label l = new Label(label);
        l.setMinWidth(110);
        l.getStyleClass().add("form-label");
        HBox box = new HBox(8, l, field);
        box.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(field, Priority.ALWAYS);
        return box;
    }

    // Creates the radio menu items of a filter menu; onPick gets the chosen text
    public static ArrayList<MenuItem> radioItems(String[] options, Consumer<String> onPick) {
        ToggleGroup group = new ToggleGroup();
        ArrayList<MenuItem> items = new ArrayList<MenuItem>();
        for (int i = 0; i < options.length; i++) {
            String option = options[i];
            RadioMenuItem item = new RadioMenuItem(option);
            item.setToggleGroup(group);
            item.setSelected(i == 0);
            item.setOnAction(e -> onPick.accept(option));
            items.add(item);
        }
        return items;
    }

    // Shows the red "Error!" card
    public static void error(String message) {
        notice("Error!", message, false);
    }

    // Shows the same card with the title "Notice"
    public static void info(String message) {
        notice("Notice", message, false);
    }

    // Shows a Yes / No question card; returns true if Yes was pressed
    public static boolean confirm(String message) {
        return notice("Are you sure?", message, true);
    }

    // Builds and shows the card used by error, info and confirm
    private static boolean notice(String title, String message, boolean askYesNo) {
        answeredYes = false;

        SVGPath bubble = new SVGPath();
        bubble.setContent("M4 4h16a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-6l-5 5v-5H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z");
        bubble.setFill(Color.web("#FF4B4B"));
        bubble.setScaleX(3.4);
        bubble.setScaleY(3.4);
        Label mark = new Label(askYesNo ? "?" : "!");
        mark.setStyle("-fx-text-fill: white; -fx-font-size: 40px; -fx-font-weight: bold;");
        mark.setTranslateY(-6);
        StackPane icon = new StackPane(new Group(bubble), mark);
        icon.setMinSize(96, 96);
        icon.setPrefSize(96, 96);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("notice-title");
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("notice-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(360);
        VBox text = new VBox(4, titleLabel, messageLabel);
        text.setAlignment(Pos.CENTER_LEFT);

        HBox top = new HBox(26, icon, text);
        top.setAlignment(Pos.CENTER_LEFT);
        top.setPadding(new Insets(28, 40, 24, 36));

        VBox card = new VBox(top);
        card.getStyleClass().add("notice-card");
        Region strip = new Region();
        strip.getStyleClass().add("notice-strip");
        strip.setMinHeight(16);
        strip.setPrefHeight(16);

        Stage stage = popupOn(activeWindow(), card);

        if (askYesNo) {
            Button yes = Dialogs.pillButton("Yes");
            Button no = Dialogs.pillButton("No");
            yes.setOnAction(e -> { answeredYes = true; stage.close(); });
            no.setOnAction(e -> stage.close());
            HBox buttons = new HBox(10, yes, no);
            buttons.setAlignment(Pos.CENTER_RIGHT);
            buttons.setPadding(new Insets(0, 24, 14, 24));
            card.getChildren().add(buttons);
        }
        card.getChildren().add(strip);

        Button close = new Button();
        close.setGraphic(Icons.outline(Icons.CLOSE, 20));
        close.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 2;");
        close.setOnAction(e -> stage.close());
        StackPane layer = new StackPane(card, close);
        StackPane.setAlignment(close, Pos.TOP_RIGHT);
        StackPane.setMargin(close, new Insets(12, 14, 0, 0));
        ((StackPane) stage.getScene().getRoot()).getChildren().setAll(layer);

        stage.getScene().setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });
        stage.showAndWait();
        return answeredYes;
    }
}
