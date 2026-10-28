package es.medac.lordo0174.tunel;

import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;

import java.util.List;
import java.util.Random;

/**
 * Lógica de la sala 5, «El Sótano».
 *
 * El visitante trae de la página HTML de 1.º la combinación de la caja fuerte
 * (está escondida en el diario del vigilante), gira las tres ruedas y pulsa ABRIR.
 * La interfaz es una pesadilla a propósito: el sótano está a oscuras y solo se ve
 * lo que alumbra la linterna, que sigue al ratón.
 */
public class SalaController {

    private static final String MENSAJE_INICIAL =
            "La bombilla se ha fundido. Solo tenéis una linterna… y el diario del vigilante.";

    private static final double ANCHO_BASE = 1280;
    private static final double ALTO_BASE = 800;
    private static final double RADIO_LINTERNA = 200;
    private static final double OSCURIDAD = 0.95;        // opacidad de la negrura fuera de la linterna

    private static final double ALTO_LADRILLO = 34;
    private static final double ANCHO_LADRILLO = 92;
    private static final double JUNTA = 4;

    @FXML private StackPane raiz;
    @FXML private StackPane escenario;
    @FXML private Pane pared;
    @FXML private HBox panelRuedas;
    @FXML private Spinner<Integer> rueda1;
    @FXML private Spinner<Integer> rueda2;
    @FXML private Spinner<Integer> rueda3;
    @FXML private Region oscuridad;
    @FXML private Label mensaje;
    @FXML private Label firma;

    private List<Spinner<Integer>> ruedas;
    private double luzX;
    private double luzY;

    @FXML
    private void initialize() {
        firma.setText("Túnel del Terror · Sala 5 · " + App.PROPIETARIO);
        mensaje.setText(MENSAJE_INICIAL);
        dibujarPared();

        ruedas = List.of(rueda1, rueda2, rueda3);
        for (Spinner<Integer> rueda : ruedas) {
            SpinnerValueFactory.IntegerSpinnerValueFactory valores =
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 0);
            valores.setWrapAround(true);   // del 9 se pasa al 0, como en una caja fuerte de verdad
            rueda.setValueFactory(valores);
            // Flecha arriba, número y flecha abajo, como una rueda. Se añade aquí porque
            // FXMLLoader solo se queda con la primera clase de la lista en un Spinner
            rueda.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_VERTICAL);
            rueda.addEventHandler(ScrollEvent.SCROLL, evento -> girarConRueda(rueda, evento));
        }

        raiz.widthProperty().addListener((obs, antes, ahora) -> ajustarAPantalla());
        raiz.heightProperty().addListener((obs, antes, ahora) -> ajustarAPantalla());
        raiz.addEventFilter(MouseEvent.MOUSE_MOVED, this::moverLinterna);
        raiz.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::moverLinterna);

        alumbrar(ANCHO_BASE / 2, ALTO_BASE / 2);
    }

    /** App lo llama cuando la ventana ya se ve: la linterna empieza apuntando a las ruedas. */
    public void iniciar() {
        raiz.applyCss();
        raiz.layout();
        alumbrar(panelRuedas);
        rueda1.requestFocus();
    }

    // ---------- la caja fuerte ----------

    @FXML
    private void abrir() {
        mensaje.setText("La manivela no se mueve. Algo la sujeta desde dentro…");
    }

    /** La rueda del ratón también gira las ruedas de la caja: hacia arriba suma, hacia abajo resta. */
    private void girarConRueda(Spinner<Integer> rueda, ScrollEvent evento) {
        if (evento.getDeltaY() == 0 || rueda.isDisabled()) {
            return;
        }
        if (evento.getDeltaY() > 0) {
            rueda.increment();
        } else {
            rueda.decrement();
        }
        evento.consume();
    }

    // ---------- la linterna ----------

    private void moverLinterna(MouseEvent evento) {
        // El escenario está escalado: se pasa la posición del ratón a sus coordenadas
        Point2D punto = escenario.sceneToLocal(evento.getSceneX(), evento.getSceneY());
        alumbrar(punto.getX(), punto.getY());
    }

    /** Apunta la linterna al centro de un nodo del escenario. */
    private void alumbrar(Node nodo) {
        Bounds limites = escenario.sceneToLocal(nodo.localToScene(nodo.getBoundsInLocal()));
        if (limites.getWidth() > 0) {
            alumbrar(limites.getCenterX(), limites.getCenterY());
        }
    }

    private void alumbrar(double x, double y) {
        luzX = x;
        luzY = y;
        pintarOscuridad();
    }

    /** La oscuridad es un degradado radial: transparente donde apunta la linterna y casi negro fuera. */
    private void pintarOscuridad() {
        RadialGradient luz = new RadialGradient(0, 0, luzX, luzY, RADIO_LINTERNA, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 214, 150, 0.04)),
                new Stop(0.6, Color.rgb(0, 0, 0, 0.35)),
                new Stop(1, Color.rgb(0, 0, 0, OSCURIDAD)));
        oscuridad.setBackground(new Background(new BackgroundFill(luz, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    // ---------- montaje de la interfaz ----------

    /** Pared de ladrillos del sótano, en hiladas desplazadas. Solo se ve donde alumbra la linterna. */
    private void dibujarPared() {
        Random tonos = new Random(5);   // semilla fija: siempre sale la misma pared
        for (int fila = 0; fila * ALTO_LADRILLO < ALTO_BASE; fila++) {
            double desfase = fila % 2 == 0 ? 0 : -ANCHO_LADRILLO / 2;
            for (double x = desfase; x < ANCHO_BASE; x += ANCHO_LADRILLO) {
                Rectangle ladrillo = new Rectangle(x + JUNTA / 2, fila * ALTO_LADRILLO + JUNTA / 2,
                        ANCHO_LADRILLO - JUNTA, ALTO_LADRILLO - JUNTA);
                double tono = tonos.nextDouble();
                ladrillo.setFill(Color.hsb(10 + tono * 12, 0.5 + 0.2 * tono, 0.22 + 0.12 * tonos.nextDouble()));
                ladrillo.setArcWidth(5);
                ladrillo.setArcHeight(5);
                pared.getChildren().add(ladrillo);
            }
        }
        pared.setClip(new Rectangle(ANCHO_BASE, ALTO_BASE));
    }

    /** El escenario mide 1280x800 y se escala para caber en cualquier pantalla. */
    private void ajustarAPantalla() {
        double escala = Math.min(raiz.getWidth() / ANCHO_BASE, raiz.getHeight() / ALTO_BASE);
        if (escala > 0) {
            escenario.setScaleX(escala);
            escenario.setScaleY(escala);
        }
    }
}
