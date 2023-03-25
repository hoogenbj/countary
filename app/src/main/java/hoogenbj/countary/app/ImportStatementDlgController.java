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
import hoogenbj.countary.util.StatementParsers;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

public class ImportStatementDlgController extends Dialog<KeyValue> {

    @FXML
    private ChoiceBox<KeyValue> parsers;

    @FXML
    private ButtonType okButtonType;

    private Button okButton;

    @Inject
    private UserInterface userInterface;

    public void initialize() {
        parsers.setItems(FXCollections.observableList(
                Arrays.stream(StatementParsers.values()).sequential().map(p -> new KeyValue(p.description(), p.name())).toList())
        );
        parsers.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                okButton.setDisable(false);
            }
        });
    }

    public static ImportStatementDlgController getInstance(Window owner) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(ImportStatementDlgController.class.getResource("ImportStatement.fxml"));
        ImportStatementDlgController controller = null;
        try {
            DialogPane dlgPane = loader.load();
            controller = loader.getController();
            controller.initOwner(owner);
            controller.setTitle("Select an account");
            controller.setDialogPane(dlgPane);
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            controller.okButton.setDisable(true);
            ImportStatementDlgController finalController = controller;
            controller.setResultConverter(buttonType -> {
                if (!Objects.equals(ButtonBar.ButtonData.OK_DONE, buttonType.getButtonData())) {
                    return null;
                }
                StatementParsers parser = StatementParsers.valueOf(finalController.parsers.getValue().value());
                String filePath = finalController.userInterface.openBankStatementFile(parser.fileExt());
                if (filePath != null) {
                    return new KeyValue(parser.name(), filePath);
                } else
                    return null;
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to launch SelectAccountDlgController", e);
        }
        return controller;
    }
}
