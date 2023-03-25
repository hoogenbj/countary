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

import hoogenbj.countary.model.Budget;
import hoogenbj.countary.model.BudgetItem;
import hoogenbj.countary.model.DataModel;
import hoogenbj.countary.model.Item;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Objects;
import java.util.Set;

public class CreateBudgetItemDlgController extends Dialog<BudgetItem> {
    @FXML
    private TextField plannedField;
    @FXML
    private TextArea noteField;
    @FXML
    private Text itemText;
    @FXML
    private Text budgetText;
    @FXML
    private Pane tagPane;
    @FXML
    private ButtonType okButtonType;

    private UserInterface ui;
    private DataModel dataModel;
    private Budget budget;
    private Item item;
    private Button okButton;

    public static CreateBudgetItemDlgController getInstance(UserInterface ui, DataModel dataModel, Budget budget, Item item) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(CreateBudgetItemDlgController.class.getResource("CreateBudgetItemDlg.fxml"));
        CreateBudgetItemDlgController controller = new CreateBudgetItemDlgController();
        controller.ui = ui;
        controller.dataModel = dataModel;
        controller.item = item;
        controller.budget = budget;
        controller.initOwner(CountaryApp.OWNER_WINDOW);
        controller.setTitle("Add an item");
        loader.setController(controller);
        try {
            DialogPane dlgPane = loader.load();
            controller.setDialogPane(dlgPane);
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            controller.okButton.addEventFilter(ActionEvent.ACTION, controller::inputValidation);
            CreateBudgetItemDlgController finalController = controller;
            controller.setResultConverter(buttonType -> {
                if (!Objects.equals(ButtonBar.ButtonData.OK_DONE, buttonType.getButtonData())) {
                    return null;
                }
                return finalController.composeResult();
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to launch CreateBudgetItem Dialog", e);
        }
        return controller;
    }

    private BudgetItem composeResult() {
        try {
            BigDecimal planned;
            if (plannedField.getText().isEmpty())
                planned = BigDecimal.ZERO;
            else
                planned = new BigDecimal(plannedField.getText());
            return dataModel.addItemToBudget(budget, item, planned, noteField.getText());
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve tags for item", e);
        }
    }

    @FXML
    private void initialize() {
        Platform.runLater(() -> plannedField.requestFocus());
        itemText.setText(String.format(" %s ", item.name()));
        budgetText.setText(String.format(" %s", budget.name()));
        tagPane.getChildren().add(TaggingControl.getInstance(ui, dataModel, Set.of(item)));
    }

    private void inputValidation(ActionEvent event) {
        if (!plannedField.getText().isEmpty()) {
            DecimalFormat decimalFormat = new DecimalFormat("#,###,###,###.00");
            try {
                decimalFormat.parse(plannedField.getText());
            } catch (ParseException e) {
                ui.showError("The planned field does not contain a valid decimal value");
                event.consume();
            }
        }
    }
}
