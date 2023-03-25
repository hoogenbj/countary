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
import hoogenbj.countary.util.DbUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.sqlite.core.Codes.SQLITE_CONSTRAINT;

public class BudgetController implements ControllerHelpers {

    private boolean alwaysShowHidden;
    private boolean tableEditable;
    private Boolean noButtons;
    private Consumer<BudgetHolder> budgetSelectionNotifier;

    @FXML
    private RadioButton annualKind;

    @FXML
    private RadioButton monthlyKind;
    @FXML
    private Button cloneButton;
    @FXML
    private Button createButton;

    @FXML
    private RadioButton adhocKind;

    @FXML
    private RadioButton noKind;

    @FXML
    private CheckBox showHidden;

    @FXML
    private ToggleGroup kindToggleGroup;

    @FXML
    private TableView<BudgetHolder> tableView;

    @FXML
    private TableColumn<BudgetHolder, String> nameColumn;

    @FXML
    private TableColumn<BudgetHolder, Kind> kindColumn;

    @FXML
    private TableColumn<BudgetHolder, Boolean> hiddenColumn;

    @FXML
    private TableColumn<BudgetHolder, BigDecimal> balanceColumn;

    @FXML
    private ClearableTextField searchCriteria;

    @Inject
    private DataModel model;

    @Inject
    private UserInterface userInterface;

    @Inject
    private Settings settings;

    private ObservableList<BudgetHolder> listOfBudgets;
    private FilteredList<BudgetHolder> filteredList;
    private final Map<BudgetHolder, Future<BigDecimal>> actuals = new HashMap<>();

    public BudgetController() {
    }

    public BudgetController(DataModel model, UserInterface userInterface, Settings settings,
                            Consumer<BudgetHolder> budgetSelectionNotifier, Boolean noButtons,
                            boolean tableEditable, boolean alwaysShowHidden) {
        this.model = model;
        this.userInterface = userInterface;
        this.settings = settings;
        this.budgetSelectionNotifier = budgetSelectionNotifier;
        this.noButtons = noButtons;
        this.tableEditable = tableEditable;
        this.alwaysShowHidden = alwaysShowHidden;
    }

    public Node createNode() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(BudgetController.class.getResource("Budget.fxml"));
        loader.setController(this);
        return loader.load();
    }

    public void initialize() {
        initControls();
        loadData();
        tableView.getItems().addListener((ListChangeListener<BudgetHolder>) (c -> {
            c.next();
            final int size = tableView.getItems().size();
            // TODO: test this with a large enough number of rows.
            if (size > 0) {
                tableView.scrollTo(size - 1);
            }
        }));
    }

    private void loadData() {
        try {
            listOfBudgets = FXCollections.observableArrayList(model.getBudgets().stream()
                    .map(budget -> new BudgetHolder(budget, this::onHiddenChanged)).toList());
            listOfBudgets.forEach(budgetHolder -> CompletableFuture.supplyAsync(() -> {
                try {
                    BigDecimal balance = model.getActualForBudget(budgetHolder.getBudget());
                    budgetHolder.setBalance(balance);
                    return balance;
                } catch (SQLException e) {
                    throw new RuntimeException("Unable to retrieve actual for budget " + budgetHolder.getBudget().name(), e);
                }
            }));
            filteredList = new FilteredList<>(listOfBudgets);
            filteredList.setPredicate(getPredicate());
            SortedList<BudgetHolder> sortedList = new SortedList<>(filteredList, Comparator.comparing(BudgetHolder::getId).reversed());
            tableView.setItems(sortedList);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load budgets", e);
        }
    }

    private Budget onHiddenChanged(Budget budget, Boolean aBoolean) {
        try {
            return model.updateBudgetHidden(budget, aBoolean);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to update hidden flag on budget " + budget.name(), e);
        }
    }

    private void initControls() {
        if (noButtons) {
            cloneButton.setManaged(false);
            cloneButton.setVisible(false);
            createButton.setVisible(false);
            createButton.setManaged(false);
        }
        annualKind.setUserData(Kind.Annual);
        monthlyKind.setUserData(Kind.Monthly);
        adhocKind.setUserData(Kind.AdHoc);
        noKind.setUserData(Kind.None);
        nameColumn.setCellValueFactory(p -> p.getValue().nameProperty());
        kindColumn.setCellValueFactory(p -> p.getValue().kindProperty());
        balanceColumn.setCellValueFactory(p -> p.getValue().balanceProperty());
        balanceColumn.setCellFactory(this::makeBigDecimalCell);
        hiddenColumn.setCellValueFactory(p -> p.getValue().hiddenProperty());
        hiddenColumn.setCellFactory(this::makeHiddenCell);
        if (alwaysShowHidden) {
            showHidden.setVisible(false);
            showHidden.setManaged(false);
        } else {
            showHidden.selectedProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && !newValue.equals(oldValue)) {
                    if (searchCriteria.getText().isEmpty())
                        filteredList.setPredicate(getPredicate());
                    else
                        doSearch(searchCriteria.getText());
                }
            });
        }
        tableView.setEditable(tableEditable);
        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                if (!noButtons)
                    cloneButton.setDisable(false);
                budgetSelectionNotifier.accept(newValue);
            } else {
                budgetSelectionNotifier.accept(null);
            }
        });
        kindToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                if (searchCriteria.getText().isEmpty())
                    filteredList.setPredicate(getPredicate());
                else
                    doSearch(searchCriteria.getText());
            }
        });
        // Remove focus from text field.
        Platform.runLater(() -> searchCriteria.getParent().requestFocus());
        searchCriteria.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.length() > 1 && !newValue.equals(oldValue)) {
                doSearch(newValue);
            } else {
                filteredList.setPredicate(getPredicate());
            }
        });
    }

    private TableCell<BudgetHolder, Boolean> makeHiddenCell(TableColumn<BudgetHolder, Boolean> column) {
        return new CheckBoxTableCell<>();
    }

    private Predicate<BudgetHolder> getPredicate() {
        return holder -> this.filterKind(holder) && this.filterHidden(holder);
    }

    private boolean filterHidden(BudgetHolder holder) {
        if (alwaysShowHidden)
            return true;
        boolean show = showHidden.isSelected();
        if (show)
            return true;
        else
            return !holder.isHidden();
    }

    private boolean filterKind(BudgetHolder holder) {
        Kind kind = (Kind) kindToggleGroup.getSelectedToggle().getUserData();
        if (kind.equals(Kind.None))
            return true;
        else
            return holder.cat().equals(kind);
    }

    private Predicate<BudgetHolder> getPredicate(Set<Long> ids) {
        return b -> this.filterHidden(b) && this.filterKind(b) && ids.contains(b.getId());
    }

    @FXML
    private void onCreateBudget() {
        CreateBudgetDlgController controller = CreateBudgetDlgController.getInstance(CountaryApp.OWNER_WINDOW);
        Optional<Budget> optional = controller.showAndWait();
        optional.ifPresent(budget -> {
            try {
                budget = model.createBudget(budget);
                Budget finalBudget = budget;
                Platform.runLater(() -> listOfBudgets.add(new BudgetHolder(finalBudget, this::onHiddenChanged)));
            } catch (SQLException e) {
                if (e.getErrorCode() == SQLITE_CONSTRAINT) {
                    userInterface.showError("A budget with that name already exists");
                } else {
                    throw new RuntimeException("Unable to create budget", e);
                }
            }
        });
    }

    @FXML
    private void onCloneBudget() {
        Budget budget = tableView.getSelectionModel().getSelectedItem().getBudget();
        CloneBudgetDlgController controller = CloneBudgetDlgController.getInstance(CountaryApp.OWNER_WINDOW);
        controller.showAndWait().ifPresent(name -> {
            try {
                Budget newBudget = model.cloneBudget(budget, name);
                listOfBudgets.add(new BudgetHolder(newBudget, this::onHiddenChanged));
            } catch (SQLException e) {
                if (e.getErrorCode() == SQLITE_CONSTRAINT) {
                    userInterface.showError("A budget with that name already exists");
                } else {
                    throw new RuntimeException("Unable to create budget", e);
                }
            }
        });
    }

    private void doSearch(String criteria) {
        try {
            if (!criteria.contains("*"))
                criteria = criteria + "*";
            Set<Long> budgets = model.searchBudgets("name", criteria);
            filteredList.setPredicate(getPredicate(budgets));
        } catch (SQLException e) {
            throw new RuntimeException("Unable to search budgets", e);
        }
    }
}
