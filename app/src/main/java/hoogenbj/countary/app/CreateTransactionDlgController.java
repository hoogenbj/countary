/*
 * Copyright (c) 2022-2023. Johan Hoogenboezem
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
import hoogenbj.countary.model.Transaction;
import hoogenbj.countary.util.InputUtils;
import hoogenbj.countary.util.ParseUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;
import javafx.stage.Window;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.ZoneOffset;
import java.util.*;

public class CreateTransactionDlgController extends Dialog<Transaction> implements Initializable {

    @FXML
    private DatePicker transactedField;
    @FXML
    private DatePicker postedField;
    @FXML
    private TextField amountField;
    @FXML
    private TextField balanceField;
    @FXML
    private TextField descriptionField;
    @FXML
    private Text accountField;
    @FXML
    private SVGPath accountTag;
    @FXML
    private ButtonType okButtonType;
    private boolean transactedHasValue = false;
    private Account account;

    private Button okButton;

    public static CreateTransactionDlgController getInstance(Window owner, Account account) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(CreateTransactionDlgController.class.getResource("CreateTransactionDlg.fxml"));
        CreateTransactionDlgController controller = new CreateTransactionDlgController();
        loader.setController(controller);
        controller.setTitle("Create a manual transaction");
        controller.initOwner(owner);
        controller.account = account;
        try {
            DialogPane dlgPane = loader.load();
            controller.setDialogPane(dlgPane);
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            controller.okButton.setDisable(true);
            CreateTransactionDlgController finalController = controller;
            controller.setResultConverter(buttonType -> {
                if(!Objects.equals(ButtonBar.ButtonData.OK_DONE, buttonType.getButtonData())) {
                    return null;
                }
                return finalController.composeResult();
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to launch CreateTransactionDlgController", e);
        }
        return controller;
    }

    private Transaction composeResult() {
        Calendar postedOn = Calendar.getInstance();
        postedOn.setTime(Date.from(postedField.getValue().atStartOfDay().toInstant(ZoneOffset.MIN)));
        Calendar transactionDate = null;
        if (transactedHasValue) {
            transactionDate = Calendar.getInstance();
            transactionDate.setTime(Date.from(transactedField.getValue().atStartOfDay().toInstant(ZoneOffset.MIN)));
        }
        String description = descriptionField.getText();
        BigDecimal amount = ParseUtils.parseBigDecimal(amountField.getText());
        BigDecimal balance = ParseUtils.parseBigDecimal(balanceField.getText());
        int transactionHash = Objects.hash(postedOn, description, amount, transactionDate, balance);
        return new Transaction(null, account, postedOn.getTime(),
                (transactionDate != null) ? transactionDate.getTime() : null, amount,
                balance, description, (long) transactionHash, false, true, true);
    }

    private enum Inputs {
        Amount, Balance, Description, PostedOn
    }
    private final EnumSet<Inputs> inputState = EnumSet.noneOf(Inputs.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        transactedField.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                transactedHasValue = true;
            }
        });
        accountField.setText(Account.toKeyValue(account).key());
        accountTag.setContent(AccountTag.svgPathContent);
        accountTag.setFill(Color.web(account.tagColor()));
        EnumSet<Inputs> all = EnumSet.allOf(Inputs.class);
        InputUtils inputUtils = new InputUtils(() -> {
            okButton.setDisable(!inputState.containsAll(all));
        });
        inputUtils.observeChangesInInput(amountField.textProperty(), inputState, Inputs.Amount);
        inputUtils.observeChangesInInput(balanceField.textProperty(), inputState, Inputs.Balance);
        inputUtils.observeChangesInInput(descriptionField.textProperty(), inputState, Inputs.Description);
        inputUtils.observeChangesInInput(postedField.valueProperty(), inputState, Inputs.PostedOn);
    }
}
