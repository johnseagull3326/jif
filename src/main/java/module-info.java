module johnseagull.jif {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens johnseagull.jif to javafx.fxml;
    exports johnseagull.jif;
}