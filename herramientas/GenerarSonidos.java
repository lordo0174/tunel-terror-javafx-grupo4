import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.function.DoubleUnaryOperator;

/**
 * Genera por síntesis todos los sonidos de la sala 5 (no hay audios descargados).
 *
 * Uso, desde la raíz del proyecto:
 *     java herramientas/GenerarSonidos.java
 *
 * Los WAV resultantes se guardan en src/main/resources/.../tunel/sonidos.
 */
public class GenerarSonidos {

    private static final float MUESTREO = 22050f;
    private static final Path DESTINO = Path.of("src/main/resources/es/medac/lordo0174/tunel/sonidos");

    // Semilla fija: cada vez que se ejecuta salen exactamente los mismos sonidos
    private static final Random azar = new Random(5);

    public static void main(String[] args) throws IOException {
        Files.createDirectories(DESTINO);
        guardar("ambiente.wav", ambiente(), 0.70);
        guardar("rueda.wav", rueda(), 0.55);
        guardar("clac.wav", clac(), 0.80);
        guardar("cerrojo.wav", cerrojo(), 0.85);
        guardar("chirrido.wav", chirrido(), 0.75);
        guardar("puerta.wav", puerta(), 0.85);
        guardar("susto.wav", susto(), 0.90);
        guardar("letra.wav", campana(), 0.80);
        guardar("latido.wav", latido(), 0.85);
        System.out.println("Sonidos generados en " + DESTINO.toAbsolutePath());
    }

    /** Dron grave, viento y gotas que caen con eco. Dura 16 s y encaja consigo mismo para sonar en bucle. */
    private static double[] ambiente() {
        int n = muestras(16);
        int cruce = muestras(1.5);
        double[] s = new double[n + cruce];
        double vientoAgudo = 0, vientoGrave = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / MUESTREO;
            // Todas las frecuencias son múltiplos de 1/16 Hz: el bucle no da saltos
            double dron = 0.35 * seno(55.0, t) + 0.28 * seno(58.25, t)
                    + 0.12 * seno(82.5, t) + 0.05 * seno(116.5, t);
            double respiracion = 0.6 + 0.4 * seno(0.125, t);

            // Viento: ruido blanco filtrado en banda que se abre y se cierra despacio
            double apertura = 0.5 + 0.5 * seno(0.0625, t);
            double ruido = azar.nextDouble() * 2 - 1;
            vientoAgudo += (0.03 + 0.05 * apertura) * (ruido - vientoAgudo);
            vientoGrave += 0.006 * (ruido - vientoGrave);
            double viento = (vientoAgudo - vientoGrave) * (0.4 + 0.6 * (0.5 + 0.5 * seno(0.1875, t)));

            s[i] = dron * respiracion + viento * 3.5;
        }
        double[] bucle = cerrarBucle(s, n, cruce);

        // Las gotas se añaden después de cerrar el bucle y lejos del empalme, para que no se corten
        double[][] gotas = {{2.1, 1150}, {5.4, 1400}, {6.0, 1050}, {9.3, 1300}, {12.8, 1200}, {14.0, 1500}};
        for (double[] gota : gotas) {
            int inicio = muestras(gota[0]);
            for (int i = inicio; i < bucle.length; i++) {
                double t = (i - inicio) / MUESTREO;
                // Eco del sótano: la misma gota repetida más flojo
                bucle[i] += 0.5 * (gota(t, gota[1]) + 0.35 * gota(t - 0.11, gota[1]) + 0.15 * gota(t - 0.23, gota[1]));
            }
        }
        return bucle;
    }

    /** Gota de agua: un "plic" cuyo tono sube muy deprisa y se apaga enseguida. */
    private static double gota(double t, double frecuencia) {
        if (t < 0 || t > 0.4) {
            return 0;
        }
        double subida = frecuencia * (1 + 0.6 * Math.min(1, t / 0.03));
        return Math.sin(2 * Math.PI * subida * t) * Math.min(1, t / 0.002) * Math.exp(-t * 32);
    }

    /** Clic de trinquete cuando una rueda avanza un número. */
    private static double[] rueda() {
        double[] s = new double[muestras(0.07)];
        for (int i = 0; i < s.length; i++) {
            double t = i / MUESTREO;
            double ruido = azar.nextDouble() * 2 - 1;
            double primero = ruido * Math.exp(-t * 400) * 0.7 + seno(2600, t) * Math.exp(-t * 250) * 0.5;
            double t2 = t - 0.018;
            double segundo = t2 < 0 ? 0 : ruido * Math.exp(-t2 * 500) * 0.35 + seno(3400, t2) * Math.exp(-t2 * 300) * 0.3;
            s[i] = primero + segundo;
        }
        return s;
    }

    /** "Clac": el cerrojo cede. Dos golpes metálicos secos y un golpe grave. */
    private static double[] clac() {
        double[] s = new double[muestras(0.45)];
        for (int i = 0; i < s.length; i++) {
            double t = i / MUESTREO;
            double t2 = t - 0.11;
            double grave = t2 < 0 ? 0 : seno(70, t2) * Math.exp(-t2 * 25) * 0.8;
            s[i] = golpeMetalico(t, 1900) + 1.3 * golpeMetalico(t2, 1500) + grave;
        }
        return s;
    }

    private static double golpeMetalico(double t, double frecuencia) {
        if (t < 0) {
            return 0;
        }
        double ruido = azar.nextDouble() * 2 - 1;
        return ruido * Math.exp(-t * 300) * 0.6
                + seno(frecuencia, t) * Math.exp(-t * 90) * 0.5
                + seno(frecuencia * 2.7, t) * Math.exp(-t * 140) * 0.3;
    }

    /** La manivela choca contra el cerrojo: golpe de metal pesado con un rebote. */
    private static double[] cerrojo() {
        double[] s = new double[muestras(1.0)];
        for (int i = 0; i < s.length; i++) {
            double t = i / MUESTREO;
            s[i] = Math.tanh(1.4 * (impactoPesado(t) + 0.5 * impactoPesado(t - 0.16)));
        }
        return s;
    }

    private static double impactoPesado(double t) {
        if (t < 0) {
            return 0;
        }
        // Parciales inarmónicos: es lo que hace que suene a metal y no a nota musical
        double[] parciales = {310, 847, 1390, 2210};
        double[] volumen = {1, 0.6, 0.4, 0.25};
        double[] caida = {6, 9, 12, 18};
        double metal = 0;
        for (int p = 0; p < parciales.length; p++) {
            metal += volumen[p] * seno(parciales[p], t) * Math.exp(-t * caida[p]);
        }
        double ruido = (azar.nextDouble() * 2 - 1) * Math.exp(-t * 40) * 0.4;
        double golpe = seno(55, t) * Math.exp(-t * 8) * 0.8;
        return (metal * 0.5 + ruido + golpe) * Math.min(1, t / 0.002);
    }

    /** Chirrido corto: algo gira una rueda en la oscuridad. */
    private static double[] chirrido() {
        return crujido(0.9, t -> 35 + 25 * t / 0.9, 780);
    }

    /** La puerta de la caja se abre despacio: chirrido largo que acelera y frena. */
    private static double[] puerta() {
        return crujido(2.0, t -> 14 + 26 * Math.sin(Math.PI * t / 2.0), 520);
    }

    /**
     * Crujido por "pega y suelta": impulsos irregulares que hacen vibrar dos resonadores,
     * como una bisagra oxidada. El ritmo de los impulsos (por segundo) varía con el tiempo.
     */
    private static double[] crujido(double segundos, DoubleUnaryOperator ritmo, double tono) {
        double[] s = new double[muestras(segundos)];
        double[] resonador1 = resonador(tono);
        double[] resonador2 = resonador(tono * 1.7);
        double y1 = 0, y1Previo = 0, y2 = 0, y2Previo = 0;
        double fase = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / MUESTREO;
            fase += ritmo.applyAsDouble(t) * (1 + 0.3 * (azar.nextDouble() - 0.5)) / MUESTREO;
            double impulso = 0;
            if (fase >= 1) {
                fase -= 1;
                impulso = 0.6 + 0.4 * azar.nextDouble();
            }
            double nuevo1 = impulso + resonador1[0] * y1 + resonador1[1] * y1Previo;
            y1Previo = y1;
            y1 = nuevo1;
            double nuevo2 = impulso + resonador2[0] * y2 + resonador2[1] * y2Previo;
            y2Previo = y2;
            y2 = nuevo2;
            double envolvente = Math.pow(Math.sin(Math.PI * t / segundos), 0.6);
            s[i] = (y1 + 0.6 * y2) * envolvente;
        }
        return s;
    }

    /** Coeficientes de un resonador de dos polos que suena a la frecuencia indicada. */
    private static double[] resonador(double frecuencia) {
        double r = 0.996;
        double w = 2 * Math.PI * frecuencia / MUESTREO;
        return new double[] {2 * r * Math.cos(w), -r * r};
    }

    /** Golpe de orquesta disonante con chirrido ascendente: el susto. */
    private static double[] susto() {
        double[] s = new double[muestras(1.8)];
        double[] cluster = {277.2, 293.7, 311.1, 415.3, 440.0, 622.3};
        double faseChirrido = 0;
        for (int i = 0; i < s.length; i++) {
            double t = i / MUESTREO;
            double ataque = Math.min(1, t / 0.01);
            double golpe = 0;
            for (double f : cluster) {
                golpe += sierra(f * (1 + 0.004 * seno(5, t)), t);
            }
            golpe = golpe / cluster.length * ataque * Math.exp(-t * 1.6);

            double fChirrido = 700 + 900 * Math.min(1, t / 0.6) + 40 * seno(9, t);
            faseChirrido += 2 * Math.PI * fChirrido / MUESTREO;
            double chirrido = Math.sin(faseChirrido) * ataque * Math.exp(-t * 2.2) * 0.5;

            double ruido = (azar.nextDouble() * 2 - 1) * ataque * Math.exp(-t * 6) * 0.5;
            s[i] = Math.tanh(1.8 * (golpe + chirrido + ruido));
        }
        return s;
    }

    /** Campana inarmónica con eco para el momento de la letra. */
    private static double[] campana() {
        double[] seca = new double[muestras(3.2)];
        double[] parciales = {1, 2.76, 5.40, 8.93};
        double[] volumen = {1, 0.6, 0.35, 0.2};
        double[] caida = {1.2, 2.0, 3.0, 4.5};
        for (int i = 0; i < seca.length; i++) {
            double t = i / MUESTREO;
            double sonido = 0.5 * seno(196, t) * Math.exp(-t * 1.0);
            for (int p = 0; p < parciales.length; p++) {
                double env = Math.exp(-t * caida[p]);
                sonido += volumen[p] * env * (seno(392 * parciales[p], t) + seno(393.5 * parciales[p], t)) * 0.5;
            }
            seca[i] = sonido * Math.min(1, t / 0.004);
        }
        double[] s = new double[seca.length];
        int eco1 = muestras(0.35);
        int eco2 = muestras(0.7);
        for (int i = 0; i < s.length; i++) {
            s[i] = seca[i]
                    + (i >= eco1 ? seca[i - eco1] * 0.4 : 0)
                    + (i >= eco2 ? seca[i - eco2] * 0.2 : 0);
        }
        return s;
    }

    /** Latido de corazón (pum-pum) para los últimos segundos. */
    private static double[] latido() {
        double[] s = new double[muestras(0.9)];
        for (int i = 0; i < s.length; i++) {
            double t = i / MUESTREO;
            s[i] = golpeCorazon(t) + 0.8 * golpeCorazon(t - 0.26);
        }
        return s;
    }

    private static double golpeCorazon(double t) {
        if (t < 0) {
            return 0;
        }
        double frecuencia = 62 - 25 * Math.min(1, t / 0.12);
        return Math.sin(2 * Math.PI * frecuencia * t) * Math.min(1, t / 0.005) * Math.exp(-t * 16);
    }

    // ---------- utilidades ----------

    private static int muestras(double segundos) {
        return (int) Math.round(segundos * MUESTREO);
    }

    private static double seno(double frecuencia, double t) {
        return Math.sin(2 * Math.PI * frecuencia * t);
    }

    private static double sierra(double frecuencia, double t) {
        double ciclo = frecuencia * t;
        return 2 * (ciclo - Math.floor(ciclo + 0.5));
    }

    /** Funde el sobrante del final con el principio para que el bucle no haga "clic". */
    private static double[] cerrarBucle(double[] s, int n, int cruce) {
        double[] r = new double[n];
        for (int i = 0; i < n; i++) {
            if (i < cruce) {
                double peso = (double) i / cruce;
                r[i] = s[i] * peso + s[n + i] * (1 - peso);
            } else {
                r[i] = s[i];
            }
        }
        return r;
    }

    /** Normaliza al pico indicado y escribe un WAV PCM de 16 bits mono. */
    private static void guardar(String nombre, double[] s, double pico) throws IOException {
        double max = 1e-9;
        for (double v : s) {
            max = Math.max(max, Math.abs(v));
        }
        byte[] bytes = new byte[s.length * 2];
        for (int i = 0; i < s.length; i++) {
            int v = (int) Math.round(s[i] / max * pico * Short.MAX_VALUE);
            bytes[2 * i] = (byte) v;
            bytes[2 * i + 1] = (byte) (v >> 8);
        }
        AudioFormat formato = new AudioFormat(MUESTREO, 16, 1, true, false);
        try (AudioInputStream entrada = new AudioInputStream(new ByteArrayInputStream(bytes), formato, s.length)) {
            AudioSystem.write(entrada, AudioFileFormat.Type.WAVE, DESTINO.resolve(nombre).toFile());
        }
    }
}
