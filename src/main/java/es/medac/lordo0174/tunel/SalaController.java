package es.medac.lordo0174.tunel;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Lógica de la sala 5, «El Sótano».
 *
 * El visitante trae de la página HTML de 1.º la combinación de la caja fuerte
 * (está escondida en el diario del vigilante), gira las tres ruedas y pulsa ABRIR.
 * La interfaz es una pesadilla a propósito: el sótano está a oscuras y solo se ve
 * lo que alumbra la linterna, que sigue al ratón; y si nadie toca la caja,
 * algo gira una rueda por su cuenta.
 */
public class SalaController {

    /** Pista acordada con 1.º: el reloj del sótano se paró a las 3:17, cuando desapareció el vigilante. */
    static final int[] COMBINACION = {3, 1, 7};
    /** Letra que entrega la sala 5. */
    static final String LETRA = "O";
    /** Ponlo a false si en el grupo hay alguien que no deba llevarse el susto. */
    static final boolean SUSTO_ACTIVADO = true;

    private static final int SEGUNDOS_PIEDAD = 60;
    private static final int SEGUNDOS_LETRA_REGALADA = 80;
    private static final int SEGUNDOS_LATIDOS = 15;      // últimos segundos antes de la piedad
    private static final int SEGUNDOS_DERIVA = 7;        // si nadie toca la caja, una rueda gira sola
    private static final int SEGUNDOS_REINICIO = 15;

    private static final String MENSAJE_INICIAL =
            "La bombilla se ha fundido. Solo tenéis una linterna… y el diario del vigilante.";
    private static final String[] MENSAJES_FALLO = {
            "%s… el cerrojo ni se inmuta.",
            "%s. Algo se ríe dentro de la caja.",
            "%s tampoco. El vigilante probó lo mismo… y sigue aquí abajo.",
            "%s no abre nada. Leed otra vez el diario."
    };

    private static final double ANCHO_BASE = 1280;
    private static final double ALTO_BASE = 800;
    private static final double RADIO_LINTERNA = 200;
    private static final double OSCURIDAD = 0.95;        // opacidad de la negrura fuera de la linterna

    private static final double ALTO_LADRILLO = 34;
    private static final double ANCHO_LADRILLO = 92;
    private static final double JUNTA = 4;

    private enum Estado { ESPERANDO, JUGANDO, PIEDAD, REVELANDO }

    @FXML private StackPane raiz;
    @FXML private StackPane escenario;
    @FXML private Pane pared;
    @FXML private StackPane caja;
    @FXML private StackPane puerta;
    @FXML private HBox panelRuedas;
    @FXML private Spinner<Integer> rueda1;
    @FXML private Spinner<Integer> rueda2;
    @FXML private Spinner<Integer> rueda3;
    @FXML private Button botonAbrir;
    @FXML private Group volante;
    @FXML private Region oscuridad;
    @FXML private Label titulo;
    @FXML private Label mensaje;
    @FXML private ProgressBar paciencia;
    @FXML private StackPane capaSusto;
    @FXML private Region sombraSusto;
    @FXML private Group cara;
    @FXML private VBox capaLetra;
    @FXML private Label letra;
    @FXML private Label cuentaReinicio;
    @FXML private Label firma;

    private List<Spinner<Integer>> ruedas;
    private final Random azar = new Random();
    private Estado estado = Estado.ESPERANDO;
    private int segundos;
    private int ultimoToque;
    private int fallos;
    private int segundosParaReiniciar;
    private boolean moviendoSolo;
    private boolean teniaCombinacion;
    private double luzX;
    private double luzY;
    private final DoubleProperty radioLuz = new SimpleDoubleProperty(RADIO_LINTERNA);

    private MediaPlayer ambiente;
    private AudioClip sonidoRueda;
    private AudioClip sonidoClac;
    private AudioClip sonidoCerrojo;
    private AudioClip sonidoChirrido;
    private AudioClip sonidoPuerta;
    private AudioClip sonidoSusto;
    private AudioClip sonidoLetra;
    private AudioClip sonidoLatido;

    private Timeline reloj;
    private Timeline cuentaAtras;
    private Timeline brilloLetra;
    private Timeline respiracionLuz;
    private Animation respiracionTitulo;
    private Animation sacudida;
    private Animation forcejeo;
    private Animation pulsoBoton;
    private Animation encendido;
    private Animation autoGiro;
    private Animation secuenciaFinal;

    @FXML
    private void initialize() {
        firma.setText("Túnel del Terror · Sala 5 · " + App.PROPIETARIO);
        letra.setText(LETRA);
        dibujarPared();
        // La sala se oscurece al mismo ritmo que aparece la cara
        sombraSusto.opacityProperty().bind(cara.opacityProperty());

        ruedas = List.of(rueda1, rueda2, rueda3);
        for (Spinner<Integer> rueda : ruedas) {
            SpinnerValueFactory.IntegerSpinnerValueFactory valores =
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 0);
            valores.setWrapAround(true);   // del 9 se pasa al 0, como en una caja fuerte de verdad
            rueda.setValueFactory(valores);
            // Flecha arriba, número y flecha abajo, como una rueda. Se añade aquí porque
            // FXMLLoader solo se queda con la primera clase de la lista en un Spinner
            rueda.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_VERTICAL);
            rueda.valueProperty().addListener((obs, antes, ahora) -> alGirarRueda());
            rueda.addEventHandler(ScrollEvent.SCROLL, evento -> girarConRueda(rueda, evento));
        }

        raiz.widthProperty().addListener((obs, antes, ahora) -> ajustarAPantalla());
        raiz.heightProperty().addListener((obs, antes, ahora) -> ajustarAPantalla());
        raiz.addEventFilter(MouseEvent.MOUSE_MOVED, this::moverLinterna);
        raiz.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::moverLinterna);
        raiz.addEventFilter(MouseEvent.MOUSE_PRESSED, evento -> empezarSiEspera());
        raiz.addEventFilter(KeyEvent.KEY_PRESSED, evento -> empezarSiEspera());
        radioLuz.addListener((obs, antes, ahora) -> pintarOscuridad());

        crearAnimaciones();
        alumbrar(ANCHO_BASE / 2, ALTO_BASE / 2);
        reiniciar();
    }

    /** Arranca el sonido y las animaciones de ambiente. App lo llama cuando la ventana ya se ve. */
    public void iniciar() {
        ambiente = crearBucle("ambiente.wav", 0.7);
        sonidoRueda = new AudioClip(recurso("rueda.wav"));
        sonidoClac = new AudioClip(recurso("clac.wav"));
        sonidoCerrojo = new AudioClip(recurso("cerrojo.wav"));
        sonidoChirrido = new AudioClip(recurso("chirrido.wav"));
        sonidoPuerta = new AudioClip(recurso("puerta.wav"));
        sonidoSusto = new AudioClip(recurso("susto.wav"));
        sonidoLetra = new AudioClip(recurso("letra.wav"));
        sonidoLatido = new AudioClip(recurso("latido.wav"));

        ambiente.play();
        respiracionTitulo.play();
        respiracionLuz.play();

        // La linterna empieza apuntando a las ruedas
        raiz.applyCss();
        raiz.layout();
        alumbrar(panelRuedas);
        rueda1.requestFocus();
    }

    /** Vuelve al estado inicial para el siguiente grupo. También se llama con F12. */
    public void reiniciar() {
        reloj.stop();
        cuentaAtras.stop();
        brilloLetra.stop();
        sacudida.stop();
        forcejeo.stop();
        if (secuenciaFinal != null) {
            secuenciaFinal.stop();
        }
        pararAyudas();

        estado = Estado.ESPERANDO;
        segundos = 0;
        ultimoToque = 0;
        fallos = 0;

        capaSusto.setVisible(false);
        capaLetra.setVisible(false);
        caja.setTranslateX(0);
        puerta.setScaleX(1);
        puerta.setTranslateX(0);
        volante.setRotate(0);
        oscuridad.setOpacity(1);
        ruedas.forEach(rueda -> {
            rueda.setDisable(false);
            rueda.setTranslateY(0);
        });
        botonAbrir.setDisable(false);
        paciencia.setProgress(1);
        colocarRuedasAlAzar();
        mensaje.setText(MENSAJE_INICIAL);
        alumbrar(panelRuedas);
    }

    /** Detiene sonidos y animaciones al cerrar la aplicación. */
    public void apagar() {
        reloj.stop();
        cuentaAtras.stop();
        respiracionLuz.stop();
        if (ambiente != null) {
            ambiente.dispose();
        }
    }

    // ---------- la caja fuerte ----------

    @FXML
    private void abrir() {
        if (estado == Estado.REVELANDO) {
            return;
        }
        empezarSiEspera();
        if (combinacionCorrecta()) {
            revelar();
        } else {
            fallar();
        }
    }

    private void alGirarRueda() {
        boolean correcta = combinacionCorrecta();
        boolean acabaDeEncajar = correcta && !teniaCombinacion;
        boolean acabaDeSoltarse = !correcta && teniaCombinacion;
        teniaCombinacion = correcta;

        // Al colocar las ruedas para el siguiente grupo no suena nada
        if (!(moviendoSolo && estado == Estado.ESPERANDO)) {
            reproducir(sonidoRueda, 0.5);
        }
        if (acabaDeEncajar) {
            reproducir(sonidoClac, 0.7);
        }

        // Solo cuenta como "toque" si la ha girado el visitante, no la propia sala
        if (moviendoSolo || estado == Estado.REVELANDO) {
            return;
        }
        empezarSiEspera();
        ultimoToque = segundos;
        if (acabaDeEncajar) {
            mensaje.setText("Clac… algo ha cedido dentro de la caja. ¡Pulsa ABRIR!");
        } else if (acabaDeSoltarse) {
            mensaje.setText(MENSAJE_INICIAL);
        }
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

    private void fallar() {
        String plantilla = MENSAJES_FALLO[fallos % MENSAJES_FALLO.length];
        fallos++;
        mensaje.setText(String.format(plantilla, combinacionActual()));
        reproducir(sonidoCerrojo, 0.9);
        sacudida.playFromStart();
        forcejeo.playFromStart();
        ultimoToque = segundos;
    }

    private boolean combinacionCorrecta() {
        for (int i = 0; i < ruedas.size(); i++) {
            if (ruedas.get(i).getValue() != COMBINACION[i]) {
                return false;
            }
        }
        return true;
    }

    private String combinacionActual() {
        return ruedas.stream()
                .map(rueda -> String.valueOf(rueda.getValue()))
                .collect(Collectors.joining(" · "));
    }

    private static String textoCombinacion(int[] cifras) {
        return Arrays.stream(cifras)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(" · "));
    }

    /** Pone una rueda en un número desde el código sin que cuente como un toque del visitante. */
    private void ponerRueda(Spinner<Integer> rueda, int numero) {
        moviendoSolo = true;
        rueda.getValueFactory().setValue(numero);
        moviendoSolo = false;
    }

    /** Cada grupo se encuentra las ruedas en una posición distinta (nunca en la buena). */
    private void colocarRuedasAlAzar() {
        do {
            ruedas.forEach(rueda -> ponerRueda(rueda, azar.nextInt(10)));
        } while (combinacionCorrecta());
        teniaCombinacion = false;
    }

    // ---------- el tiempo: rueda fantasma, piedad y letra regalada ----------

    private void empezarSiEspera() {
        if (estado == Estado.ESPERANDO) {
            estado = Estado.JUGANDO;
            segundos = 0;
            ultimoToque = 0;
            reloj.playFromStart();
        }
    }

    /** Se ejecuta cada segundo desde que el grupo toca la sala. */
    private void tic() {
        segundos++;
        paciencia.setProgress(Math.max(0, 1 - (double) segundos / SEGUNDOS_PIEDAD));

        if (estado == Estado.JUGANDO) {
            if (segundos >= SEGUNDOS_PIEDAD) {
                apiadarse();
                return;
            }
            if (segundos > SEGUNDOS_PIEDAD - SEGUNDOS_LATIDOS) {
                reproducir(sonidoLatido, 0.8);
            }
            if (segundos - ultimoToque >= SEGUNDOS_DERIVA && !combinacionCorrecta()) {
                moverseSolo();
            }
        } else if (estado == Estado.PIEDAD && segundos >= SEGUNDOS_LETRA_REGALADA) {
            revelar();
        }
    }

    /** Si nadie toca la caja, algo gira una rueda en la oscuridad. */
    private void moverseSolo() {
        Spinner<Integer> rueda = ruedas.get(azar.nextInt(ruedas.size()));
        int salto = (1 + azar.nextInt(2)) * (azar.nextBoolean() ? 1 : -1);
        ponerRueda(rueda, Math.floorMod(rueda.getValue() + salto, 10));
        reproducir(sonidoChirrido, 0.8);
        temblar(rueda);
        mensaje.setText("Algo ha girado una de las ruedas en la oscuridad…");
        ultimoToque = segundos;
    }

    /** A los 60 s la sala se apiada: se enciende la bombilla y las ruedas giran solas hasta la combinación. */
    private void apiadarse() {
        estado = Estado.PIEDAD;
        mensaje.setText("El vigilante se apiada de vosotros: enciende la bombilla y gira las ruedas…");

        FadeTransition bombilla = new FadeTransition(Duration.seconds(2.5), oscuridad);
        bombilla.setToValue(0);
        encendido = bombilla;
        bombilla.play();

        Timeline giro = new Timeline(new KeyFrame(Duration.millis(170), evento -> girarHaciaLaCombinacion()));
        giro.setCycleCount(Animation.INDEFINITE);
        giro.setDelay(Duration.seconds(1));
        autoGiro = giro;
        giro.play();
    }

    /** Cada paso de la ayuda avanza un número la primera rueda que todavía no está en el suyo. */
    private void girarHaciaLaCombinacion() {
        for (int i = 0; i < ruedas.size(); i++) {
            Spinner<Integer> rueda = ruedas.get(i);
            if (rueda.getValue() != COMBINACION[i]) {
                ponerRueda(rueda, (rueda.getValue() + 1) % 10);
                return;
            }
        }
        autoGiro.stop();
        mensaje.setText("Ya está: " + textoCombinacion(COMBINACION) + ". Pulsad ABRIR.");
        pulsoBoton.play();
    }

    private void pararAyudas() {
        if (encendido != null) {
            encendido.stop();
        }
        if (autoGiro != null) {
            autoGiro.stop();
        }
        moviendoSolo = false;
        pulsoBoton.stop();
        botonAbrir.setScaleX(1);
        botonAbrir.setScaleY(1);
    }

    // ---------- el final: puerta, susto controlado y letra ----------

    private void revelar() {
        estado = Estado.REVELANDO;
        reloj.stop();
        pararAyudas();
        ruedas.forEach(rueda -> rueda.setDisable(true));
        botonAbrir.setDisable(true);

        secuenciaFinal = crearApertura();
        secuenciaFinal.setOnFinished(evento -> asustar());
        secuenciaFinal.play();
    }

    /** La manivela da media vuelta, el cerrojo cede y la puerta se abre hacia la izquierda. */
    private Animation crearApertura() {
        // La puerta "gira" sobre la bisagra izquierda: se estrecha y se desplaza para que ese borde no se mueva
        double mitad = puerta.getWidth() / 2;
        return new Timeline(
                new KeyFrame(Duration.ZERO, evento -> reproducir(sonidoClac, 0.9)),
                new KeyFrame(Duration.millis(700), evento -> reproducir(sonidoPuerta, 0.9),
                        new KeyValue(volante.rotateProperty(), -180, Interpolator.EASE_BOTH),
                        new KeyValue(puerta.scaleXProperty(), 1),
                        new KeyValue(puerta.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(2200),
                        new KeyValue(puerta.scaleXProperty(), 0.06, Interpolator.EASE_IN),
                        new KeyValue(puerta.translateXProperty(), -mitad * 0.94, Interpolator.EASE_IN),
                        new KeyValue(oscuridad.opacityProperty(), 0.3)));
    }

    private void asustar() {
        if (!SUSTO_ACTIVADO) {
            mostrarLetra();
            return;
        }
        // La cara empieza pequeña e invisible para que no aparezca de golpe ni un solo fotograma
        cara.setOpacity(0);
        cara.setScaleX(0.2);
        cara.setScaleY(0.2);
        capaSusto.setVisible(true);
        reproducir(sonidoSusto, 0.9);
        secuenciaFinal = crearSusto();
        secuenciaFinal.setOnFinished(evento -> mostrarLetra());
        secuenciaFinal.play();
    }

    /** Aparición y escala, sin destellos: la cara sale de la caja creciendo y se desvanece. */
    private Animation crearSusto() {
        return new Timeline(
                new KeyFrame(Duration.millis(350),
                        new KeyValue(cara.opacityProperty(), 1, Interpolator.EASE_OUT),
                        new KeyValue(cara.scaleXProperty(), 1.15, Interpolator.EASE_OUT),
                        new KeyValue(cara.scaleYProperty(), 1.15, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.millis(1600),
                        new KeyValue(cara.opacityProperty(), 1),
                        new KeyValue(cara.scaleXProperty(), 1.3),
                        new KeyValue(cara.scaleYProperty(), 1.3)),
                new KeyFrame(Duration.millis(2300),
                        new KeyValue(cara.opacityProperty(), 0, Interpolator.EASE_IN)));
    }

    private void mostrarLetra() {
        capaSusto.setVisible(false);
        capaLetra.setOpacity(0);
        letra.setScaleX(3);
        letra.setScaleY(3);
        segundosParaReiniciar = SEGUNDOS_REINICIO;
        cuentaReinicio.setText(textoReinicio());
        capaLetra.setVisible(true);
        reproducir(sonidoLetra, 0.9);

        FadeTransition fundido = new FadeTransition(Duration.seconds(1.4), capaLetra);
        fundido.setToValue(1);
        ScaleTransition acercamiento = new ScaleTransition(Duration.seconds(1.4), letra);
        acercamiento.setToX(1);
        acercamiento.setToY(1);
        acercamiento.setInterpolator(Interpolator.EASE_OUT);

        secuenciaFinal = new ParallelTransition(fundido, acercamiento);
        secuenciaFinal.setOnFinished(evento -> cuentaAtras.playFromStart());
        secuenciaFinal.play();
        brilloLetra.play();
    }

    private void ticReinicio() {
        segundosParaReiniciar--;
        if (segundosParaReiniciar <= 0) {
            reiniciar();
        } else {
            cuentaReinicio.setText(textoReinicio());
        }
    }

    private String textoReinicio() {
        return "El sótano se cerrará en " + segundosParaReiniciar + " s";
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
        RadialGradient luz = new RadialGradient(0, 0, luzX, luzY, radioLuz.get(), false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 214, 150, 0.04)),
                new Stop(0.6, Color.rgb(0, 0, 0, 0.35)),
                new Stop(1, Color.rgb(0, 0, 0, OSCURIDAD)));
        oscuridad.setBackground(new Background(new BackgroundFill(luz, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    // ---------- montaje de la interfaz ----------

    private void crearAnimaciones() {
        FadeTransition respiracion = new FadeTransition(Duration.seconds(2.6), titulo);
        respiracion.setFromValue(1);
        respiracion.setToValue(0.45);
        respiracion.setAutoReverse(true);
        respiracion.setCycleCount(Animation.INDEFINITE);
        respiracionTitulo = respiracion;

        // La linterna "respira": el círculo de luz se abre y se cierra despacio, nunca parpadea
        respiracionLuz = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(radioLuz, RADIO_LINTERNA)),
                new KeyFrame(Duration.seconds(2.8), new KeyValue(radioLuz, RADIO_LINTERNA * 1.12, Interpolator.EASE_BOTH)));
        respiracionLuz.setAutoReverse(true);
        respiracionLuz.setCycleCount(Animation.INDEFINITE);

        TranslateTransition temblor = new TranslateTransition(Duration.millis(45), caja);
        temblor.setFromX(-10);
        temblor.setToX(10);
        temblor.setAutoReverse(true);
        temblor.setCycleCount(6);
        temblor.setOnFinished(evento -> caja.setTranslateX(0));
        sacudida = temblor;

        // Al fallar, la manivela intenta girar y rebota contra el cerrojo
        RotateTransition tiron = new RotateTransition(Duration.millis(120), volante);
        tiron.setFromAngle(0);
        tiron.setToAngle(25);
        tiron.setAutoReverse(true);
        tiron.setCycleCount(4);
        tiron.setOnFinished(evento -> volante.setRotate(0));
        forcejeo = tiron;

        ScaleTransition pulso = new ScaleTransition(Duration.millis(600), botonAbrir);
        pulso.setFromX(1);
        pulso.setFromY(1);
        pulso.setToX(1.08);
        pulso.setToY(1.08);
        pulso.setAutoReverse(true);
        pulso.setCycleCount(Animation.INDEFINITE);
        pulsoBoton = pulso;

        // Brillo que "respira" alrededor de la letra: lento, nunca parpadea
        DropShadow brillo = new DropShadow(BlurType.GAUSSIAN, Color.web("#9e0000"), 30, 0.2, 0, 0);
        letra.setEffect(brillo);
        brilloLetra = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(brillo.radiusProperty(), 25)),
                new KeyFrame(Duration.seconds(2), new KeyValue(brillo.radiusProperty(), 75, Interpolator.EASE_BOTH)));
        brilloLetra.setAutoReverse(true);
        brilloLetra.setCycleCount(Animation.INDEFINITE);

        reloj = new Timeline(new KeyFrame(Duration.seconds(1), evento -> tic()));
        reloj.setCycleCount(Animation.INDEFINITE);

        cuentaAtras = new Timeline(new KeyFrame(Duration.seconds(1), evento -> ticReinicio()));
        cuentaAtras.setCycleCount(Animation.INDEFINITE);
    }

    /** La rueda que ha movido "algo" tiembla un instante. */
    private void temblar(Spinner<Integer> rueda) {
        TranslateTransition temblor = new TranslateTransition(Duration.millis(40), rueda);
        temblor.setFromY(-5);
        temblor.setToY(5);
        temblor.setAutoReverse(true);
        temblor.setCycleCount(6);
        temblor.setOnFinished(evento -> rueda.setTranslateY(0));
        temblor.play();
    }

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

    // ---------- sonido ----------

    private static MediaPlayer crearBucle(String archivo, double volumen) {
        MediaPlayer reproductor = new MediaPlayer(new Media(recurso(archivo)));
        reproductor.setCycleCount(MediaPlayer.INDEFINITE);
        reproductor.setVolume(volumen);
        reproductor.setOnError(() -> System.err.println("Error de sonido en " + archivo + ": " + reproductor.getError()));
        return reproductor;
    }

    private static String recurso(String archivo) {
        return SalaController.class.getResource("sonidos/" + archivo).toExternalForm();
    }

    private static void reproducir(AudioClip efecto, double volumen) {
        if (efecto != null) {
            efecto.play(volumen);
        }
    }
}
