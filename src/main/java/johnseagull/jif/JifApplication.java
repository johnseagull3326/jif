package johnseagull.jif;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class JifApplication extends Application {
    public static Stage s;

    @Override
    public void start(Stage stage) throws IOException {
        s = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(JifApplication.class.getResource("jif-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 685, 513);
        stage.setTitle("JIF encoder / decoder");
        stage.setScene(scene);
        stage.show();
        stage.setResizable(false);
    }
}
