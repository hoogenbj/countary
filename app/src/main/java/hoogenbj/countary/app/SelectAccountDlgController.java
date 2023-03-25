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

import com.google.inject.Inject;
import hoogenbj.countary.model.DataModel;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Window;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class SelectAccountDlgController extends Dialog<KeyValue> {

    @FXML
    private ButtonType okButtonType;

    @FXML
    private ChoiceBox<KeyValue> accounts;

    private Button okButton;

    @Inject
    private DataModel model;

    public SelectAccountDlgController() {
    }

    public void initialize() {
        List<KeyValue> accountsList = null;
        try {
            accountsList = model.getAccountsList();
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve list of accounts from database", e);
        }
        accounts.setItems(FXCollections.observableList(accountsList));
        accounts.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                okButton.setDisable(false);
            }
        });
    }

    public static SelectAccountDlgController getInstance(Window owner) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(SelectAccountDlgController.class.getResource("SelectAccountDlg.fxml"));
        SelectAccountDlgController controller = null;
        try {
            DialogPane dlgPane = loader.load();
            controller = loader.getController();
            controller.initOwner(owner);
            controller.setTitle("Select an account");
            controller.setDialogPane(dlgPane);
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            controller.okButton.setDisable(true);
            SelectAccountDlgController finalController = controller;
            controller.setResultConverter(buttonType -> {
                if(!Objects.equals(ButtonBar.ButtonData.OK_DONE, buttonType.getButtonData())) {
                    return null;
                }
                return finalController.accounts.getValue();
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to launch SelectAccountDlgController", e);
        }
        return controller;
    }
}
