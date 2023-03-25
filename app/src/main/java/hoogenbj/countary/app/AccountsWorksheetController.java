/*
 * Copyright (c) 2023. Johan Hoogenboezem
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
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Predicate;

public class AccountsWorksheetController {
    @FXML
    private TableColumn<AccountHolder, String> nameColumn;
    @FXML
    private TableColumn<AccountHolder, String> numberColumn;
    @FXML
    private TableColumn<AccountHolder, String> branchColumn;
    @FXML
    private TableColumn<AccountHolder, String> bankColumn;
    @FXML
    private TableColumn<AccountHolder, Account> tagColumn;
    @Inject
    private DataModel model;

    @Inject
    private UserInterface userInterface;

    @Inject
    private Settings settings;

    @FXML
    private TableView<AccountHolder> tableView;
    private FilteredList<AccountHolder> filteredList;
    private boolean searchingText;
    private ObservableList<AccountHolder> listOfAccounts;
    private Set<Long> accounts = Collections.emptySet();
    @FXML
    private ClearableTextField searchCriteria;

    public AccountsWorksheetController() {
    }

    public void initialize() {
        initControls();
        loadData();
        tableView.getItems().addListener((ListChangeListener<AccountHolder>) (c -> {
            c.next();
            final int size = tableView.getItems().size();
            // TODO: test this with a large enough number of rows.
            if (size > 0) {
                tableView.scrollTo(size - 1);
            }
        }));
    }

    private void initControls() {
        nameColumn.setCellValueFactory(p -> p.getValue().nameProperty());
        numberColumn.setCellValueFactory(p -> p.getValue().numberProperty());
        branchColumn.setCellValueFactory(p -> p.getValue().branchProperty());
        bankColumn.setCellValueFactory(p -> p.getValue().bankProperty());
        tagColumn.setCellFactory(this::makeTag);
        tagColumn.setCellValueFactory(p -> p.getValue().accountProperty());
        Platform.runLater(() -> searchCriteria.getParent().requestFocus());
        searchCriteria.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.length() > 1 && !newValue.equals(oldValue)) {
                searchingText = true;
                doSearch(newValue);
                updatePredicate();
            } else {
                searchingText = false;
                accounts = Collections.emptySet();
                updatePredicate();
            }
        });
    }

    private TableCell<AccountHolder, Account> makeTag(TableColumn<AccountHolder, Account> column) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Account account, boolean empty) {
                super.updateItem(account, empty);
                this.setText(null);
                this.setGraphic(null);
                if (!empty && account != null) {
                    SVGPath tag = new SVGPath();
                    tag.setContent(AccountTag.svgPathContent);
                    tag.setFill(Color.web(account.tagColor()));
                    this.setGraphic(tag);
                }
            }
        };
    }

    private void doSearch(String criteria) {
        try {
            if (!criteria.contains("*"))
                criteria = criteria + "*";
            accounts = model.searchAccounts(criteria);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to search accounts", e);
        }
    }

    private void updatePredicate() {
        filteredList.setPredicate(getPredicate());
    }

    private Predicate<? super AccountHolder> getPredicate() {
        return this::filterText;
    }

    private boolean filterText(AccountHolder holder) {
        if (!searchingText)
            return true;
        else
            return accounts.contains(holder.getId());
    }

    private void loadData() {
        try {
            listOfAccounts = FXCollections.observableArrayList(model.getAccounts().stream()
                    .map(AccountHolder::new).toList());
//            listOfAccounts.forEach(accountHolder -> CompletableFuture.supplyAsync(() -> {
//                try {
//                    BigDecimal balance = model.getActualForBudget(accountHolder.getBudget());
//                    accountHolder.setBalance(balance);
//                    return balance;
//                } catch (SQLException e) {
//                    throw new RuntimeException("Unable to retrieve actual for budget " + accountHolder.getBudget().name(), e);
//                }
//            }));
            filteredList = new FilteredList<>(listOfAccounts);
            SortedList<AccountHolder> sortedList = new SortedList<>(filteredList, Comparator.comparing(AccountHolder::getName).reversed());
            tableView.setItems(sortedList);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load budgets", e);
        }
    }
}
