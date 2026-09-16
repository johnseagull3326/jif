package johnseagull.jif;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class ErrorController {

    @FXML
    public TextArea stack;
    @FXML
    public Label info;

    public void fin(MouseEvent mouseEvent) {
        Platform.exit();
    }
    public void retry(MouseEvent mouseEvent) {
        info.setText("But it refused.");

        PauseTransition reset = new PauseTransition(Duration.seconds(0.4));
        reset.setOnFinished(event -> {
            Platform.runLater(()->{
                Stage stage = JifApplication.s;
                stage.hide();
                FXMLLoader fxmlLoader = new FXMLLoader(JifApplication.class.getResource("jif-view.fxml"));
                try {
                    Scene scene = new Scene(fxmlLoader.load(), 685, 513);
                    stage.setTitle("JIF encoder / decoder");
                    stage.setScene(scene);
                    stage.show();
                    stage.setResizable(false);

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
        });
        reset.play();

    }
}