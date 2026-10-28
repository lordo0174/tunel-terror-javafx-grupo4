package es.medac.lordo0174.tunel;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Túnel del Terror · Sala 5 · El Sótano.
 * Arranca la sala a pantalla completa, sin aviso y sin salida accidental.
 */
public class App extends Application {

    /** Usuario de GitHub del autor de la sala. */
    public static final String PROPIETARIO = "lordo0174";

    /** Combinación secreta para que el monitor de la sala pueda cerrarla. */
    private static final KeyCombination SALIR =
            new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);

    private SalaController controlador;

    @Override
    public void start(Stage stage) throws IOException {
        // Las fuentes se cargan antes que el FXML para que el CSS ya las encuentre
        cargarFuente("Creepster-Regular.ttf");
        cargarFuente("SpecialElite-Regular.ttf");

        FXMLLoader loader = new FXMLLoader(App.class.getResource("sala.fxml"));
        Parent raiz = loader.load();
        controlador = loader.getController();

        Scene escena = new Scene(raiz, 1280, 800);
        escena.addEventFilter(KeyEvent.KEY_PRESSED, evento -> {
            if (SALIR.match(evento)) {
                Platform.exit();
            }
        });

        stage.setTitle("Túnel del Terror · Sala 5 · " + PROPIETARIO);
        stage.setScene(escena);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setFullScreen(true);
        stage.show();

        controlador.iniciar();
    }

    private static void cargarFuente(String archivo) {
        try (InputStream entrada = App.class.getResourceAsStream("fuentes/" + archivo)) {
            if (entrada == null || Font.loadFont(entrada, 20) == null) {
                System.err.println("No se pudo cargar la fuente " + archivo);
            }
        } catch (IOException e) {
            System.err.println("No se pudo cargar la fuente " + archivo + ": " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
