package hoogenbj.countary.app;

import com.google.inject.Injector;
import hoogenbj.countary.model.DataModel;
import hoogenbj.countary.util.DbMigration;
import hoogenbj.countary.util.DbUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

public class DatabaseMigrationController {

    @FXML
    private Button backupButton;
    @FXML
    private TextField from;
    @FXML
    private TextField to;
    @FXML
    private TextArea console;
    private int currentDbVersion;
    private DataModel model;
    private UserInterface userInterface;
    private Settings settings;

    public static DatabaseMigrationController getInstance(Injector injector, Stage stage, int currentDbVersion) {
        DatabaseMigrationController controller = null;
        FXMLLoader loader = injector.getInstance(FXMLLoader.class);
        loader.setLocation(DatabaseMigrationController.class.getResource("DatabaseMigrationController.fxml"));
        try {
            Parent root = loader.load();
            controller = loader.getController();
            controller.currentDbVersion = currentDbVersion;
            controller.model = injector.getInstance(DataModel.class);
            controller.userInterface = injector.getInstance(UserInterface.class);
            controller.settings = injector.getInstance(Settings.class);
            controller.init();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Database Migration");
            stage.show();
            scene.getWindow().setOnCloseRequest((WindowEvent t) -> {
                if (t.getEventType().equals(WindowEvent.WINDOW_CLOSE_REQUEST)) {
                    Platform.exit();
                }
            });
        } catch (IOException e) {
            if (controller != null)
                throw new RuntimeException("Unable to launch "+controller.getClass().getSimpleName(), e);
            else
                throw new RuntimeException("Unable to launch Console window");
        }
        return controller;
    }

    public void startMigration(ActionEvent actionEvent) {
        new DbMigration(model, userInterface, (line) -> console.appendText(line+"\n")).migrate(currentDbVersion+1);
    }

    public void cancelMigration(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void backup(ActionEvent actionEvent) {
        String backupFile = DbUtils.backupCurrentDatabase(model, userInterface, settings);
        if (backupFile != null)
            console.appendText(String.format("Current database backed up to: %s\n",backupFile));
    }

    public void restore(ActionEvent actionEvent) {
        String backupFile = DbUtils.restoreFromDatabase(model, userInterface, settings);
        if (backupFile != null)
            console.appendText(String.format("Database restored from: %s\n",backupFile));
    }

    public void init() {
        from.setText(currentDbVersion+"");
        to.setText((currentDbVersion+1)+"");
        Platform.runLater(() -> backupButton.requestFocus());
    }
}
