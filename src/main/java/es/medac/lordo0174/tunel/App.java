package es.medac.lordo0174.tunel;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Túnel del Terror · Sala 5 · El Sótano.
 */
public class App extends Application {

    /** Usuario de GitHub del autor de la sala. */
    public static final String PROPIETARIO = "lordo0174";

    @Override
    public void start(Stage stage) {
        Label aviso = new Label("Sala 5 · El Sótano (en obras)");
        Scene escena = new Scene(new StackPane(aviso), 1280, 800);

        stage.setTitle("Túnel del Terror · Sala 5 · " + PROPIETARIO);
        stage.setScene(escena);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
