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
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class AllocationTransactionToItemController extends StackPane implements ControllerHelpers {
    @FXML
    private TableView<AllocationHolder> tableView;
    @FXML
    private TableColumn<AllocationHolder, String> budgetColumn;
    @FXML
    private TableColumn<AllocationHolder, String> itemColumn;
    @FXML
    private TableColumn<AllocationHolder, String> itemAmountColumn;
    @FXML
    private TableColumn<AllocationHolder, String> noteColumn;
    @FXML
    private TableColumn<AllocationHolder, Allocation> allocationDeleteColumn;
    @FXML
    private Text transaction;
    @FXML
    private Text amount;
    @FXML
    private SVGPath accountTag;
    @FXML
    private Text balance;
    private final Map<Allocation, Future<Button>> deleteButtonLookup = new HashMap<>();
    private ObservableList<AllocationHolder> listOfItems;
    private List<Allocation> allocations;
    private DataModel model;
    private Consumer<Allocation> deletedAllocation;
    private Map<Long, AllocationHolder> allocationLookup;

    public static AllocationTransactionToItemController getInstance(DataModel model, List<Allocation> allocations,
                                                                    Consumer<Allocation> deletedAllocation) {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(DisplayTagsControl.class.getResource("AllocationTransactionToItem.fxml"));
        AllocationTransactionToItemController root = new AllocationTransactionToItemController();
        root.allocations = allocations;
        root.model = model;
        root.deletedAllocation = deletedAllocation;
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(root);
        try {
            return fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void initialize() {
        initControls();
        loadData();
    }

    private void initControls() {
        allocationDeleteColumn.setCellFactory(this::makeDeleteButton);
        allocationDeleteColumn.setCellValueFactory(p -> p.getValue().allocationProperty());
        allocationDeleteColumn.getStyleClass().add("delete-column");
        itemAmountColumn.setCellValueFactory(p -> p.getValue().amountProperty());
        noteColumn.setCellValueFactory(p -> p.getValue().noteProperty());
        budgetColumn.setCellValueFactory(p -> p.getValue().budgetNameProperty());
        itemColumn.setCellValueFactory(p -> p.getValue().itemNameProperty());
        itemColumn.setCellFactory(this::makeStringCell);
        noteColumn.prefWidthProperty()
                .bind(tableView.widthProperty()
                        .subtract(allocationDeleteColumn.widthProperty())
                        .subtract(itemAmountColumn.widthProperty()).multiply(0.33).subtract(2.0));
        itemColumn.prefWidthProperty()
                .bind(tableView.widthProperty()
                        .subtract(allocationDeleteColumn.widthProperty())
                        .subtract(itemAmountColumn.widthProperty()).multiply(0.33).subtract(2.0));
        budgetColumn.prefWidthProperty()
                .bind(tableView.widthProperty()
                        .subtract(allocationDeleteColumn.widthProperty())
                        .subtract(itemAmountColumn.widthProperty()).multiply(0.33).subtract(2.0));
    }

    private void loadData() {
        Transaction tx = allocations.get(0).transaction();
        transaction.setText(tx.description());
        accountTag.setContent(AccountTag.svgPathContent);
        accountTag.setFill(Color.web(tx.account().tagColor()));
        BigDecimal totalAllocations = allocations.stream()
                .map(Allocation::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        amount.setText(ParseUtils.formatBigDecimal(tx.amount()));
        balance.setText(ParseUtils.formatBigDecimal(tx.amount().subtract(totalAllocations)));
        listOfItems = FXCollections.observableArrayList(allocations.stream().map(AllocationHolder::new).toList());
        allocationLookup = new HashMap<>();
        listOfItems.forEach(holder -> allocationLookup.put(holder.getAllocation().id(), holder));
        SortedList<AllocationHolder> sortedList = new SortedList<>(listOfItems, Comparator.comparing(AllocationHolder::getAmount));
        tableView.setItems(sortedList);
    }

    private TableCell<AllocationHolder, Allocation> makeDeleteButton(TableColumn<AllocationHolder, Allocation> tableColumn) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Allocation allocation, boolean empty) {
                super.updateItem(allocation, empty);
                this.setText(null);
                this.setGraphic(null);
                if (!empty && allocation != null) {
                    this.setGraphic(makeButton(allocation));
                }
            }
        };
    }

    private Button makeButton(Allocation allocation) {
        Button btn = new Button();
        btn.getStyleClass().add("delete-button");
        btn.setOnAction(event -> {
            try {
                model.deleteAllocation(allocation);
                deletedAllocation.accept(allocation);
                Platform.runLater(() -> {
                    listOfItems.remove(allocationLookup.get(allocation.id()));
                    tableView.refresh();
                });
            } catch (SQLException e) {
                throw new RuntimeException("Cannot delete allocation");
            }
        });
        return btn;
    }
}
