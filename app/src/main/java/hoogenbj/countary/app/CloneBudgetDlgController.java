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

import hoogenbj.countary.util.InputUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.util.EnumSet;
import java.util.Objects;
import java.util.ResourceBundle;

public class CloneBudgetDlgController extends Dialog<String> implements Initializable {

    @FXML
    private TextField name;
    @FXML
    private ButtonType okButtonType;

    private Button okButton;

    public static CloneBudgetDlgController getInstance(Window owner) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(CreateBudgetDlgController.class.getResource("CloneBudgetDlg.fxml"));
        CloneBudgetDlgController controller = null;
        try {
            DialogPane dlgPane = loader.load();
            controller = loader.getController();
            controller.initOwner(owner);
            controller.setTitle("Copy a budget");
            controller.setDialogPane(dlgPane);
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            controller.okButton.setDisable(true);
            CloneBudgetDlgController finalController = controller;
            controller.setResultConverter(buttonType -> {
                if(!Objects.equals(ButtonBar.ButtonData.OK_DONE, buttonType.getButtonData())) {
                    return null;
                }
                return finalController.composeResult();
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to launch "+controller.getClass().getSimpleName(), e);
        }
        return controller;
    }

    private String composeResult() {
        return name.getText();
    }

    private enum Inputs {
        Name
    }
    private final EnumSet<Inputs> inputState = EnumSet.noneOf(Inputs.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        EnumSet<Inputs> all = EnumSet.allOf(Inputs.class);
        InputUtils<CloneBudgetDlgController.Inputs> inputUtils = new InputUtils<>(() -> {
            okButton.setDisable(!inputState.containsAll(all));
        });
        inputUtils.observeChangesInInput(name.textProperty(), inputState, CloneBudgetDlgController.Inputs.Name);
        Platform.runLater(() -> name.requestFocus());
    }
}
