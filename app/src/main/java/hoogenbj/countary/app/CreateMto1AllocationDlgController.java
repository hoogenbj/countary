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
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Window;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class CreateMto1AllocationDlgController extends Dialog<List<Allocation>> implements Initializable, ControllerHelpers {
    @FXML
    private TextField budgetNameField;
    @FXML
    private TextField itemNameField;
    @FXML
    private TextField noteField;
    @FXML
    private TextField allocationAmountField;
    @FXML
    private ButtonType okButtonType;

    @FXML
    private TableView<TransactionHolder> tableView;

    @FXML
    private TableColumn<TransactionHolder, BigDecimal> amountColumn;

    @FXML
    private TableColumn<TransactionHolder, String> descriptionColumn;

    private Button okButton;
    private List<AllocationHolder> allocations = new ArrayList<>();
    private BudgetItem budgetItem;
    private List<TransactionHolder> transactions;
    private FilteredList<TransactionHolder> filteredList;

    private BigDecimal sumTransactions;

    public static CreateMto1AllocationDlgController getInstance(Window owner, List<TransactionHolder> transactions,
                                                                BudgetItem budgetItem) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(CreateMto1AllocationDlgController.class.getResource("CreateMto1AllocationDlg.fxml"));
        CreateMto1AllocationDlgController controller = new CreateMto1AllocationDlgController();
        loader.setController(controller);
        controller.initOwner(owner);
        controller.setTitle("Allocate many transactions to one item");
        controller.transactions = transactions;
        controller.budgetItem = budgetItem;
        try {
            DialogPane dlgPane = loader.load();
            controller.setDialogPane(dlgPane);
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            CreateMto1AllocationDlgController finalController = controller;
            controller.setResultConverter(buttonType -> {
                if (!Objects.equals(ButtonBar.ButtonData.OK_DONE, buttonType.getButtonData())) {
                    return null;
                }
                return finalController.composeResult();
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to launch " + CreateMto1AllocationDlgController.class.getSimpleName(), e);
        }
        return controller;
    }

    private List<Allocation> composeResult() {
        return transactions.stream().map(transactionHolder ->
                new Allocation(transactionHolder.getTransaction(), budgetItem,
                        transactionHolder.getTransaction().amount(), noteField.getText())
        ).toList();
    }

    private void loadData() {
        filteredList = new FilteredList<>(FXCollections.observableList(transactions));
        SortedList<TransactionHolder> sortedList = new SortedList<>(filteredList, (left, right) -> right.pdate().compareTo(left.pdate()));
        tableView.setItems(sortedList);
        tableView.refresh();
        sumTransactions = transactions.stream()
                .map(holder -> holder.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        allocationAmountField.setText(ParseUtils.formatBigDecimal(sumTransactions));
        itemNameField.setText(budgetItem.item().name());
        budgetNameField.setText(budgetItem.budget().name());
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        amountColumn.setCellValueFactory(p -> p.getValue().amountProperty());
        amountColumn.setCellFactory(this::makeBigDecimalCell);
        descriptionColumn.setCellValueFactory(p -> p.getValue().descriptionProperty());
        loadData();
    }
}
