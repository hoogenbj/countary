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
import hoogenbj.countary.model.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class TransactionWorkSheetController implements ControllerHelpers {
    @Inject
    private Settings settings;

    @Inject
    private DataModel model;
    @Inject
    private UserInterface userInterface;
    private TransactionController transactionController;
    @FXML
    private GridPane gridPane;
    private AnchorPane budgetItemPane;
    private BudgetItemWorksheetController budgetItemWorksheetController;
    private AnchorPane allocations;
    private AllocationItemToTransactionController allocationWorksheet;
    private BudgetSummaryController budgetSummaryController;
    private SummaryController summaryController;
    private AnchorPane rightReportPane;
    private AnchorPane leftReportPane;

    public void initialize() {
        try {
            initControls();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load pane", e);
        }
    }

    private void initControls() throws IOException {
        gridPane.getChildren().addAll(getTransactionsPane(), getBudgetPane(), getBudgetItemPane(), getReportPane());
    }

    private Node getBudgetItemPane() {
        budgetItemPane = new AnchorPane();
        budgetItemPane.setPrefWidth(860);
        budgetItemPane.setPrefHeight(293);
        budgetItemPane.getStyleClass().add("make-node-outline");
        GridPane.setConstraints(budgetItemPane, 6, 4, 6, 4);
        budgetItemPane.getChildren().add(getLabeledNode("Select a budget..."));
        return budgetItemPane;
    }

    private Node getBudgetPane() throws IOException {
        BudgetController budgetController = new BudgetController(model, userInterface, settings,
                this::budgetSelectionListener, true, false, false);
        Node budgetNode = budgetController.createNode();
        anchorLayout(budgetNode);
        AnchorPane budgetPane = new AnchorPane();
        budgetPane.setPrefHeight(293);
        budgetPane.setPrefWidth(860);
        GridPane.setConstraints(budgetPane, 6, 0, 6, 4);
        budgetPane.getChildren().add(budgetNode);
        return budgetPane;
    }

    private Node getTransactionsPane() throws IOException {
        transactionController = new TransactionController(settings, userInterface, model,
                this::onBudgetItemAllocation, this::transactionSelectionListener, this::onStatementLoaded);
        Node transactionsNode = transactionController.createNode();
        AnchorPane transactionsPane = new AnchorPane();
        anchorLayout(transactionsNode);
        transactionsPane.setPrefHeight(586);
        transactionsPane.setPrefWidth(860);
        GridPane.setConstraints(transactionsPane, 0, 0, 6, 8);
        transactionsPane.getChildren().add(transactionsNode);
        return transactionsPane;
    }

    private void onStatementLoaded(Account account) {
        summaryController.update(account);
    }

    private Node getReportPane() {
        AnchorPane reportPane = new AnchorPane();
        reportPane.setPrefHeight(293);
        reportPane.setPrefWidth(1720);
        GridPane.setConstraints(reportPane, 0, 8, 12, 4);
        HBox reports = new HBox();
        anchorLayout(reports);
        allocations = new AnchorPane();
        allocations.setPrefHeight(293);
        allocations.setPrefWidth(723.3);
        allocations.getStyleClass().add("make-node-outline");
        allocations.getChildren().add(getLabeledNode("Select a transaction or a budget item..."));
        reports.getChildren().addAll(getLeftReportPane(), allocations, getRightReportPane());
        reportPane.getChildren().add(reports);
        return reportPane;
    }

    private Node getLeftReportPane() {
        leftReportPane = new AnchorPane();
        leftReportPane.setPrefHeight(293);
        leftReportPane.setPrefWidth(423.3);
        summaryController = SummaryController.getInstance(model);
        anchorLayout(summaryController);
        leftReportPane.getChildren().add(summaryController);
        return leftReportPane;
    }

    private Node getRightReportPane() {
        rightReportPane = new AnchorPane();
        rightReportPane.setPrefHeight(293);
        rightReportPane.setPrefWidth(573.3);
        HBox.setHgrow(rightReportPane, Priority.ALWAYS);
        return rightReportPane;
    }

    private void onBudgetItemAllocation(Account account, BudgetItem budgetItem) {
        if (budgetItemWorksheetController == null)
            return;
        budgetItemWorksheetController.onBudgetItemChanged(budgetItem);
        try {
            if (allocationWorksheet != null) {
                allocationWorksheet.updateAllocations(model.getAllocationsByBudgetItem(budgetItem));
            } else {
                displayAllocationWorksheet(model.getAllocationsByBudgetItem(budgetItem));
            }
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Unable to update allocation on worksheet for budget item %s", budgetItem.item().name()), e);
        }
        summaryController.update(account, budgetItem.budget());
    }

    private void budgetSelectionListener(BudgetHolder budgetHolder) {
        allocations.getChildren().clear();
        budgetItemPane.getStyleClass().clear();
        budgetItemPane.getChildren().clear();
        rightReportPane.getChildren().clear();
        transactionController.setBudgetItemSelected(null);
        if (budgetHolder != null) {
            updateBudgetItemPane(budgetHolder);
            updateBudgetSummaryPane(budgetHolder);
        } else {
            budgetItemPane.getChildren().clear();
        }
    }

    private void updateBudgetSummaryPane(BudgetHolder budgetHolder) {
        Node busySummary = makeBusyIndicator();
        rightReportPane.getChildren().add(busySummary);
        budgetSummaryController = new BudgetSummaryController(userInterface, model,
                budgetHolder.getBudget(), budgetItemWorksheetController.getFilteredBudgetItemList(),
                budgetItemWorksheetController.getCategoryModel());
        budgetItemWorksheetController.setProfileMap(budgetSummaryController.getProfileMap());
        try {
            Node budgetSummary = budgetSummaryController.createNode();
            anchorLayout(budgetSummary);
            rightReportPane.getChildren().add(budgetSummary);
            rightReportPane.getChildren().remove(busySummary);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load BudgetSummary", e);
        }
    }

    private void updateBudgetItemPane(BudgetHolder budgetHolder) {
        Node busyItems = makeBusyIndicator();
        budgetItemPane.getChildren().add(busyItems);
        Node node = getBudgetItems(budgetHolder);
        anchorLayout(node);
        budgetItemPane.getChildren().add(node);
        budgetItemPane.getChildren().remove(busyItems);
    }

    private void transactionSelectionListener(TransactionHolder transactionHolder) {
        allocationWorksheet = null;
        if (transactionHolder == null) {
            resetAllocationsPane();
        } else {
            try {
                List<Allocation> allocationList = model.getAllocations(transactionHolder.getTransaction().id());
                if (allocationList.isEmpty()) {
                    resetAllocationsPane();
                    return;
                }
                allocations.getChildren().clear();
                Node busy = makeBusyIndicator();
                allocations.getChildren().add(busy);
                AllocationTransactionToItemController allocationWorksheet = AllocationTransactionToItemController
                        .getInstance(model, allocationList, this::onAllocationDeleted);
                anchorLayout(allocationWorksheet);
                Platform.runLater(() -> {
                    allocations.getChildren().add(allocationWorksheet);
                    allocations.getChildren().remove(busy);
                });
            } catch (SQLException e) {
                throw new RuntimeException(String.format("Unable to get allocations for transaction %s", transactionHolder.getDescription()), e);
            }
        }
    }

    private void resetAllocationsPane() {
        allocations.getChildren().clear();
        allocations.getChildren().add(getLabeledNode("Select a transaction or a budget item..."));
    }

    private Node getBudgetItems(BudgetHolder budgetHolder) {
        try {
            budgetItemWorksheetController = new BudgetItemWorksheetController(userInterface, model,
                    budgetHolder, true, true, this::monitorBudgetItemSelection);
            return budgetItemWorksheetController.createNode();
        } catch (IOException e) {
            throw new RuntimeException("Unable to open BudgetWorksheetController", e);
        }
    }

    private void monitorBudgetItemSelection(BudgetItemHolder budgetItemHolder) {
        transactionController.setBudgetItemSelected(budgetItemHolder);
        if (budgetItemHolder == null) {
            allocations.getChildren().clear();
            allocations.getChildren().add(getLabeledNode("Select a transaction or a budget item..."));
            return;
        }
        try {
            List<Allocation> allocationList = model.getAllocationsByBudgetItem(budgetItemHolder.getBudgetItem());
            if (allocationList.isEmpty()) {
                allocations.getChildren().clear();
                allocations.getChildren().add(getLabeledNode("Select a transaction or a budget item..."));
                return;
            }
            displayAllocationWorksheet(allocationList);
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Unable to get allocations for budgetItem %s", budgetItemHolder.getName()), e);
        }
    }

    private void displayAllocationWorksheet(List<Allocation> allocationList) {
        allocations.getChildren().clear();
        Node busy = makeBusyIndicator();
        allocations.getChildren().add(busy);
        allocationWorksheet = AllocationItemToTransactionController
                .getInstance(model, allocationList, this::onAllocationDeleted);
        anchorLayout(allocationWorksheet);
        Platform.runLater(() -> {
            allocations.getChildren().add(allocationWorksheet);
            allocations.getChildren().remove(busy);
        });
    }

    private void onAllocationDeleted(Allocation allocation) {
        try {
            // get new Transaction so we take changes to allocation(s) into account
            transactionController.onTransactionChanged(model.getTransaction(allocation.transaction().id()));
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Unable to retrieve transaction %s",
                    allocation.transaction().description()), e);
        }
        try {
            if (budgetItemWorksheetController != null)
                // get new BudgetItem so we take changes to allocation(s) into account
                budgetItemWorksheetController.onBudgetItemChanged(model.getBudgetItem(allocation.budgetItem().id()));
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Unable to retrieve budget item %s",
                    allocation.budgetItem().item().name()), e);
        }
        summaryController.update(allocation.transaction().account(), allocation.budgetItem().budget());
    }
}
