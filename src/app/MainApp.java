package app;

import java.util.ArrayList;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import ui.LoginView;
import ui.MainView;

// Starts the JavaFX version of The Porking Lot (uses the same PorkingLotSystem as ConsoleApp)
// INHERITANCE: every JavaFX program extends Application, and start() below is the method JavaFX calls for us.
public class MainApp extends Application {
    private PorkingLotSystem system = new PorkingLotSystem();
    private Scene scene;

    // Loads the saved data and opens the login screen
    @Override
    public void start(Stage stage) {
        ArrayList<String> messages = system.startUp();
        for (String message : messages) {
            System.out.println("[INFO] " + message);
        }

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        scene = new Scene(new LoginView(system, () -> showMain()),
                Math.min(1440, screen.getWidth()), Math.min(1024, screen.getHeight()));
        scene.getStylesheets().add(getClass().getResource("/ui/style.css").toExternalForm());

        stage.setTitle("The Porking Lot");
        stage.setMinWidth(1100);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }

    // After a successful login: shows the sidebar and the pages
    private void showMain() {
        scene.setRoot(new MainView(system, () -> showLogin()));
    }

    // After Logout: shows the login screen again
    private void showLogin() {
        scene.setRoot(new LoginView(system, () -> showMain()));
    }

    // Launches the JavaFX application
    public static void main(String[] args) {
        launch(args);
    }
}
