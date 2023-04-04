/*
 * Copyright (c) 2022. Johan Hoogenboezem
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package hoogenbj.countary.app;

import com.google.inject.Injector;
import hoogenbj.countary.model.DataModel;
import hoogenbj.countary.util.DbUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

public class CountaryController implements ControllerHelpers {

    @FXML
    private void onTransactionsClicked() {
        borderPane.setCenter(getTransactions());
    }

    @FXML
    private void onBudgetsClicked() {
        borderPane.setCenter(getBudgets());
    }

    @FXML
    private void onItemsClicked() {
        borderPane.setCenter(getItems());
    }

    @FXML
    private void onDatabasesClicked() {
        borderPane.setCenter(getDatabases());
    }

    private Node getDatabases() {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(this.getClass().getResource("DatabasesWorksheet.fxml"));
        try {
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private void onAccountsClicked() {
        borderPane.setCenter(getAccounts());
    }

    private Node getAccounts() {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(this.getClass().getResource("AccountsWorksheet.fxml"));
        try {
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    private BorderPane borderPane;

    public void initialize() {
    }

    public void show(Injector injector, Stage stage) {
        UserInterface userInterface = injector.getInstance(UserInterface.class);
        if (dbInitialised(injector)) {
            loadController(injector, stage);
        } else {
            userInterface.showWarning("Unfortunately it is not possible to continue without a database");
        }
    }

    private void loadController(Injector injector, Stage stage) {
        FXMLLoader loader = injector.getInstance(FXMLLoader.class);
        loader.setLocation(this.getClass().getResource("Countary.fxml"));
        Parent root;
        try {
            root = loader.load();
            CountaryController controller = loader.getController();
            controller.borderPane.setCenter(getTransactions());
            Scene scene = new Scene(root);
            scene.getStylesheets().add(CountaryController.class.getResource("skin.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("ountary - the opinionated budget program");
            stage.show();
            scene.getWindow().setOnCloseRequest((WindowEvent t) -> {
                if (t.getEventType().equals(WindowEvent.WINDOW_CLOSE_REQUEST)) {
                    Platform.exit();
                }
            });
            CountaryApp.OWNER_WINDOW = scene.getWindow();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getOrSetDatabaseUrl(DataModel model, Settings settings, UserInterface userInterface) {
        String dbUrl = settings.getDatabaseUrl();
        if (!DbUtils.validUrl(dbUrl)) {
            String choice = userInterface.chooseDB();
            switch (choice) {
                case "Open Database" -> DbUtils.openDatabase(settings, userInterface);
                case "Open Demo Database" -> DbUtils.openDemoDatabase(settings, userInterface, model);
                case "Create New Database" -> DbUtils.createDatabase(settings, userInterface, model);
                default -> {
                }
            }
            return settings.getDatabaseUrl();
        } else {
            settings.setDatabasePath(dbUrl.substring(dbUrl.lastIndexOf(":") + 1));
        }
        return dbUrl;
    }


    private boolean dbInitialised(Injector injector) {
        Settings settings = injector.getInstance(Settings.class);
        DataModel model = injector.getInstance(DataModel.class);
        UserInterface userInterface = injector.getInstance(UserInterface.class);
        return getOrSetDatabaseUrl(model, settings, userInterface) != null;
    }

    private Node getTransactions() {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(this.getClass().getResource("TransactionWorksheet.fxml"));
        try {
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Node getBudgets() {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(this.getClass().getResource("BudgetWorksheet.fxml"));
        try {
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Node getItems() {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(this.getClass().getResource("Items.fxml"));
        try {
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onCategoriesClicked(ActionEvent actionEvent) {
        borderPane.setCenter(getCategories());
    }

    private Node getCategories() {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(this.getClass().getResource("CategoryLite.fxml"));
        try {
            return loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
