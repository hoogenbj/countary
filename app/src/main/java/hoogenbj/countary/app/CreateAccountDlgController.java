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

import hoogenbj.countary.model.Account;
import hoogenbj.countary.util.InputUtils;
import hoogenbj.countary.util.ParseUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.util.EnumSet;
import java.util.Objects;
import java.util.ResourceBundle;

public class CreateAccountDlgController extends Dialog<Account> implements Initializable, ControllerHelpers {

    @FXML
    private TextField branchCode;
    @FXML
    private SVGPath tag;
    @FXML
    private TextField bankName;
    @FXML
    private TextField accountNumber;
    @FXML
    private TextField accountName;
    @FXML
    private ButtonType okButtonType;
    @FXML
    private ColorPicker tagColor;

    private Button okButton;

    public static CreateAccountDlgController getInstance(Window owner) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(CreateAccountDlgController.class.getResource("CreateAccountDlg.fxml"));
        CreateAccountDlgController controller = null;
        try {
            DialogPane dlgPane = loader.load();
            controller = loader.getController();
            controller.initOwner(owner);
            controller.setTitle("Create an account");
            controller.setDialogPane(dlgPane);
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            controller.okButton.setDisable(true);
            CreateAccountDlgController finalController = controller;
            controller.setResultConverter(buttonType -> {
                if(!Objects.equals(ButtonBar.ButtonData.OK_DONE, buttonType.getButtonData())) {
                    return null;
                }
                return finalController.composeResult();
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to launch CreateAccountDlgController", e);
        }
        return controller;
    }

    private Account composeResult() {
        return new Account(null, accountName.getText(), accountNumber.getText(), branchCode.getText(),
                bankName.getText(), ParseUtils.toRGBCode(tagColor.getValue()));
    }

    @FXML
    private void onColorPicked(ActionEvent event) {
        tag.setFill(((ColorPicker) event.getSource()).getValue());
    }

    private enum Inputs {
        AccountName, AccountNumber, BankName, BranchCode
    }

    private final EnumSet<Inputs> inputState = EnumSet.noneOf(Inputs.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Settings settings = CountaryApp.injector.getInstance(Settings.class);
        manageCustomColors(settings, tagColor);
        EnumSet<Inputs> all = EnumSet.allOf(Inputs.class);
        InputUtils inputUtils = new InputUtils(() -> okButton.setDisable(!inputState.containsAll(all)));
        inputUtils.observeChangesInInput(accountName.textProperty(), inputState, Inputs.AccountName);
        inputUtils.observeChangesInInput(accountNumber.textProperty(), inputState, Inputs.AccountNumber);
        inputUtils.observeChangesInInput(bankName.textProperty(), inputState, Inputs.BankName);
        inputUtils.observeChangesInInput(branchCode.textProperty(), inputState, Inputs.BranchCode);
    }
}
