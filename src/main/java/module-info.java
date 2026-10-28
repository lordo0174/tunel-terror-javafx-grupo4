module es.medac.lordo0174.tunel {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;

    opens es.medac.lordo0174.tunel to javafx.fxml;
    exports es.medac.lordo0174.tunel;
}
