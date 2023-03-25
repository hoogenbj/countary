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
import hoogenbj.countary.model.BudgetHolder;
import hoogenbj.countary.model.DataModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

import java.io.IOException;

public class BudgetWorksheetController implements ControllerHelpers {

    @FXML
    private GridPane gridPane;
    private AnchorPane budgetItemPane;

    @Inject
    private DataModel model;

    @Inject
    private UserInterface userInterface;

    @Inject
    private Settings settings;
    private AnchorPane reportPane;

    public void initialize() {
        initControls();
    }

    private void initControls() {
        try {
            gridPane.getChildren().addAll(getBudgetPane(), getBudgetItemPane(), getReportPane());
        } catch (IOException e) {
            throw new RuntimeException("Unable to load budgets pane", e);
        }
    }

    private Node getBudgetPane() throws IOException {
        BudgetController budgetController = new BudgetController(model, userInterface, settings,
                this::budgetSelectionListener, false, true, true);
        Node budgetNode = budgetController.createNode();
        anchorLayout(budgetNode);
        AnchorPane budgetPane = new AnchorPane();
        budgetPane.setPrefHeight(293);
        budgetPane.setPrefWidth(1720);
        GridPane.setConstraints(budgetPane, 0, 0, 6, 4);
        budgetPane.getChildren().add(budgetNode);
        return budgetPane;
    }

    private Node getBudgetItemPane() {
        budgetItemPane = new AnchorPane();
        budgetItemPane.setPrefWidth(1720);
        budgetItemPane.setPrefHeight(293);
        budgetItemPane.getStyleClass().add("make-node-outline");
        GridPane.setConstraints(budgetItemPane, 0, 4, 6, 4);
        budgetItemPane.getChildren().add(makeBigLabel());
        return budgetItemPane;
    }

    private Node getReportPane() {
        reportPane = new AnchorPane();
        reportPane.setPrefHeight(293);
        reportPane.setPrefWidth(1720);
        GridPane.setConstraints(reportPane, 0, 8, 6, 4);
        return reportPane;
    }

    private Node makeBigLabel() {
        return getLabeledNode("Select a budget...");
    }

    private void budgetSelectionListener(BudgetHolder budgetHolder) {
        if (budgetHolder != null) {
            budgetItemPane.getStyleClass().clear();
            budgetItemPane.getChildren().clear();
            Node busyItems = makeBusyIndicator();
            budgetItemPane.getChildren().add(busyItems);
            BudgetItemWorksheetController budgetItemWorksheetController = getBudgetItems(budgetHolder);
            try {
                Node node = budgetItemWorksheetController.createNode();
                anchorLayout(node);
                Platform.runLater(() -> {
                    budgetItemPane.getChildren().add(node);
                    budgetItemPane.getChildren().remove(busyItems);
                });
            } catch (IOException e) {
                throw new RuntimeException("Unable to load BudgetItemWorksheet", e);
            }
            reportPane.getChildren().clear();
            Node busySummary = makeBusyIndicator();
            reportPane.getChildren().add(busySummary);
            BudgetSummaryController budgetSummaryController = new BudgetSummaryController(userInterface, model,
                    budgetHolder.getBudget(), budgetItemWorksheetController.getFilteredBudgetItemList(),
                    budgetItemWorksheetController.getCategoryModel());
            try {
                Node node = budgetSummaryController.createNode();
                anchorLayout(node);
                Platform.runLater(() -> {
                    reportPane.getChildren().add(node);
                    reportPane.getChildren().remove(busySummary);
                });
            } catch (IOException e) {
                throw new RuntimeException("Unable to load BudgetSummary", e);
            }
        } else {
            Platform.runLater(() -> {
                budgetItemPane.getChildren().clear();
                reportPane.getChildren().clear();
            });
        }
    }

    private BudgetItemWorksheetController getBudgetItems(BudgetHolder budgetHolder) {
        return new BudgetItemWorksheetController(userInterface, model, budgetHolder, false, false);
    }
}
