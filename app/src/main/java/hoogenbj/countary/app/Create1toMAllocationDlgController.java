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

import hoogenbj.countary.model.*;
import hoogenbj.countary.util.ParseUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Window;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class Create1toMAllocationDlgController extends Dialog<Allocation> implements Initializable {
    @FXML
    private Text transactionDescription;
    @FXML
    private Text transactionAmount;
    @FXML
    private TextField noteField;
    @FXML
    private TableColumn<AllocationHolder, String> budgetNameColumn;
    @FXML
    private TableColumn<AllocationHolder, String> amountColumn;
    @FXML
    private TextField allocationAmountField;
    @FXML
    private TextField itemNameField;
    @FXML
    private TextField budgetNameField;
    @FXML
    private ButtonType okButtonType;

    @FXML
    private TableColumn<AllocationHolder, String> itemNameColumn;

    @FXML
    private TableView<AllocationHolder> tableView;

    private Button okButton;
    private List<AllocationHolder> allocations;
    private Transaction transaction;
    private FilteredList<AllocationHolder> filteredList;

    private BudgetItem budgetItem;
    private BigDecimal balanceToAllocate;

    public static Create1toMAllocationDlgController getInstance(Window owner, Transaction transaction,
                                                                BigDecimal balanceToAllocate, BudgetItem budgetItem,
                                                                List<AllocationHolder> allocations) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(Create1toMAllocationDlgController.class.getResource("Create1toMAllocationDlg.fxml"));
        Create1toMAllocationDlgController controller = new Create1toMAllocationDlgController();
        loader.setController(controller);
        controller.initOwner(owner);
        controller.setTitle("Allocate one transaction to one or many");
        controller.allocations = allocations;
        controller.budgetItem = budgetItem;
        controller.balanceToAllocate = balanceToAllocate;
        controller.transaction = transaction;
        try {
            DialogPane dlgPane = loader.load();
            controller.setDialogPane(dlgPane);
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            Create1toMAllocationDlgController finalController = controller;
            controller.setResultConverter(buttonType -> {
                if(!Objects.equals(ButtonBar.ButtonData.OK_DONE, buttonType.getButtonData())) {
                    return null;
                }
                return finalController.composeResult();
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to launch " + Create1toMAllocationDlgController.class.getSimpleName(), e);
        }
        return controller;
    }

    private Allocation composeResult() {
        BigDecimal amount = ParseUtils.parseBigDecimal(allocationAmountField.getText());
        return new Allocation(transaction, budgetItem, amount, noteField.getText());
    }

    public void loadData() {
        filteredList = new FilteredList<>(FXCollections.observableList(allocations));
        SortedList<AllocationHolder> sortedList = new SortedList<>(filteredList, Comparator.comparing(AllocationHolder::getAmount));
        tableView.setItems(sortedList);
        budgetNameField.setText(budgetItem.budget().name());
        itemNameField.setText(budgetItem.item().name());
        allocationAmountField.setText(ParseUtils.formatBigDecimal(balanceToAllocate));
        transactionDescription.setText(transaction.description().trim());
        transactionAmount.setText(ParseUtils.formatBigDecimal(transaction.amount()));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        budgetNameColumn.setCellValueFactory(f -> f.getValue().budgetNameProperty());
        itemNameColumn.setCellValueFactory(f -> f.getValue().itemNameProperty());
        amountColumn.setCellValueFactory(f -> f.getValue().amountProperty());
        loadData();
        allocationAmountField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                BigDecimal value = ParseUtils.parseBigDecimal(newValue);
                okButton.setDisable(value == null || value.abs().compareTo(balanceToAllocate.abs()) > 0 ||
                        value.signum() != balanceToAllocate.signum());
            }
        });
        Platform.runLater(() -> allocationAmountField.requestFocus());
    }
}
