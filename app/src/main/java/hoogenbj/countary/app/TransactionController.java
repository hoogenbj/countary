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
import hoogenbj.countary.util.*;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static hoogenbj.countary.util.DbUtils.MAX_TRANSACTION_ROWS;

public class TransactionController implements ControllerHelpers {
    private Consumer<Account> onStatementLoaded;
    private Consumer<TransactionHolder> onTransactionSelected;
    private BiConsumer<Account, BudgetItem> onAllocation;
    @FXML
    private Button oneToMany;
    @FXML
    private Button refresh;
    @FXML
    private Button addTransaction;
    @FXML
    private Button manyToOne;
    @FXML
    ComboBox<Account> accounts;
    private Settings settings;
    private UserInterface userInterface;
    private DataModel model;
    ObservableList<Account> accountsList;
    ObservableList<TransactionHolder> listOfTransactions;
    private Boolean multipleTransactionsSelected = false;
    private Boolean singleTransactionsSelected = false;
    @FXML
    private CheckBox showCompletedAlso;
    @FXML
    private TableView<TransactionHolder> tableView;
    @FXML
    Button loadStatementButton;
    @FXML
    private TableColumn<TransactionHolder, String> postingDateColumn;

    @FXML
    private TableColumn<TransactionHolder, String> txDateColumn;
    @FXML
    private TableColumn<TransactionHolder, BigDecimal> amountColumn;
    @FXML
    private TableColumn<TransactionHolder, BigDecimal> balanceColumn;
    @FXML
    private TableColumn<TransactionHolder, String> descriptionColumn;
    @FXML
    private TableColumn<TransactionHolder, Transaction> deleteColumn;
    @FXML
    private ClearableTextField searchCriteria;
    @FXML
    private Button clear;
    @FXML
    private DatePicker searchDatePicker;
    private BudgetItemHolder budgetItemSelected;
    private Map<Transaction, TransactionHolder> holderLookup = new HashMap<>();
    @FXML
    private Button calculate;

    private TransactionModel transactionModel;

    public void setBudgetItemSelected(BudgetItemHolder budgetItemSelected) {
        this.budgetItemSelected = budgetItemSelected;
        applyToToolbox();
    }

    public TransactionController(Settings settings, UserInterface userInterface, DataModel model,
                                 BiConsumer<Account, BudgetItem> onAllocation,
                                 Consumer<TransactionHolder> onTransactionSelected,
                                 Consumer<Account> onStatementLoaded) {
        this.settings = settings;
        this.userInterface = userInterface;
        this.model = model;
        this.onAllocation = onAllocation;
        this.onTransactionSelected = onTransactionSelected;
        this.onStatementLoaded = onStatementLoaded;
    }

    public TransactionController() {
    }

    public Node createNode() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(TransactionController.class.getResource("Transactions.fxml"));
        loader.setController(this);
        return loader.load();
    }

    public void initialize() {
        try {
            if (!model.tableExists("account")) {
                userInterface.showWarning("The database appears to be empty. Create database objects first.");
            } else {
                transactionModel = new TransactionModel(model, this::refreshTransactions, this::searchClearable);
                initControls();
                KeyValue recentAccount = settings.getCurrentAccount();
                if (recentAccount != null) {
                    Account account = model.getAccount(Long.parseLong(recentAccount.value()));
                    if (account != null) {
                        accounts.setValue(account);
                        transactionModel.setAccount(account);
                        loadStatementButton.setDisable(false);
                    } else {
                        disableIfNoAccountIsCurrent(true);
                    }
                } else {
                    disableIfNoAccountIsCurrent(true);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve last statement from database", e);
        }
    }

    private void disableIfNoAccountIsCurrent(boolean value) {
        showCompletedAlso.setDisable(value);
        refresh.setDisable(value);
        searchCriteria.setDisable(value);
        addTransaction.setDisable(value);
    }

    private void searchClearable(Boolean aBoolean) {
        clear.setDisable(!aBoolean);
    }

    private void refreshTransactions(List<Transaction> transactions) {
        if (transactions.size() == MAX_TRANSACTION_ROWS) {
            Platform.runLater(() -> userInterface.showNotification(
                    String.format("Maximum number of transactions (%d) retrieved. Narrow your search.",
                            MAX_TRANSACTION_ROWS)));
        }
        List<TransactionHolder> list = transactions
                .stream().map(TransactionHolder::new).toList();
        holderLookup.clear();
        list.forEach(holder -> holderLookup.put(holder.getTransaction(), holder));
        listOfTransactions = FXCollections.observableArrayList(list);
        SortedList<TransactionHolder> sortedList = new SortedList<>(listOfTransactions, (left, right) -> right.pdate().compareTo(left.pdate()));
        tableView.setItems(sortedList);
        tableView.refresh();
    }

    private void initControls() throws SQLException {
        searchDatePicker.setConverter(convertSearchDate());
        searchDatePicker.valueProperty().addListener(this::dateBasedSearch);
        clear.setOnAction(btn -> clearAction());
        calculate.setOnAction(this::calculate);
        refresh.setGraphic(SVGUtils.makeRefreshIcon());
        refresh.setOnAction(btn -> transactionModel.refresh());
        oneToMany.setDisable(true);
        manyToOne.setDisable(true);
        oneToMany.setOnAction(action -> mapOneTransactionToManyBudgetItems());
        manyToOne.setOnAction(action -> mapManyTransactionsToOneBudgetItem());
        searchCriteria.textProperty().addListener(this::textBasedSearch);
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<? super TransactionHolder>) c -> monitorSelectionChanges(c.getList()));
        tableView.setRowFactory(this::makeRow);
        showCompletedAlso.selectedProperty().addListener(this::listenToShowCompletedAlsoChanges);
        postingDateColumn.setCellValueFactory(p -> p.getValue().postingDateProperty());
        txDateColumn.setCellValueFactory(p -> p.getValue().txDateProperty());
        amountColumn.setCellValueFactory(p -> p.getValue().amountProperty());
        amountColumn.setCellFactory(this::makeBigDecimalCell);
        balanceColumn.setCellValueFactory(p -> p.getValue().balanceProperty());
        balanceColumn.setCellFactory(this::makeBigDecimalCell);
        deleteColumn.setCellFactory((column) -> this.makeDeleteButton(column, this::makeButtonGraphic));
        deleteColumn.setCellValueFactory(p -> p.getValue().transactionProperty());
        deleteColumn.getStyleClass().add("delete-column");
        descriptionColumn.setCellValueFactory(p -> p.getValue().descriptionProperty());
        accountsList = FXCollections.observableList(model.getAccounts());
        accounts.setConverter(getAccountStringConverter());
        accounts.valueProperty().addListener(this::listenToAccountsChanges);
        accounts.setCellFactory(this::makeComboBoxAccountCell);
        accounts.setButtonCell(makeComboBoxAccountCell(null));
        accounts.setItems(new SortedList<>(accountsList, Comparator.comparing(o -> Account.toKeyValue(o).key())));
        // Make the description column take all available space in the table
        descriptionColumn.prefWidthProperty()
                .bind(tableView.widthProperty()
                        .subtract(deleteColumn.widthProperty())
                        .subtract(postingDateColumn.widthProperty())
                        .subtract(txDateColumn.widthProperty())
                        .subtract(amountColumn.widthProperty())
                        .subtract(balanceColumn.widthProperty()).subtract(4.0 * 5.0));
    }

    private void calculate(ActionEvent actionEvent) {
        List<BigDecimal> values = tableView.getSelectionModel().getSelectedItems().stream().map(TransactionHolder::getAmount).toList();
        CalculateDlgController dialog = CalculateDlgController.getInstance(CountaryApp.OWNER_WINDOW, values);
        dialog.showAndWait();
    }

    private StringConverter<TransactionModel.SearchChoice> choiceConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(TransactionModel.SearchChoice object) {
                return object.getKeyValue().key();
            }

            @Override
            // Not needed
            public TransactionModel.SearchChoice fromString(String string) {
                return null;
            }
        };
    }

    private Future<Button> makeButtonGraphic(TransactionHolder item) {
        return CompletableFuture.supplyAsync(() -> {
            Button btn = new Button();
            btn.setGraphic(SVGUtils.makeDeleteIcon());
            btn.disableProperty().bind(item.canDeleteProperty().not());
            btn.setOnAction(event -> {
                try {
                    if (model.canDeleteTransaction(item.getTransaction())) {
                        model.deleteTransaction(item.getTransaction());
                        Platform.runLater(() -> {
                            listOfTransactions.remove(holderLookup.get(item.getTransaction()));
                            tableView.refresh();
                        });
                    } else {
                        userInterface.showError("Cannot delete transaction. Remove it from all allocations first");
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Cannot check whether it is safe to delete transaction or delete it");
                }
            });
            return btn;
        });
    }

    private TableRow<TransactionHolder> makeRow(TableView<TransactionHolder> tableView) {
        return new TableRow<>() {
            @Override
            protected void updateItem(TransactionHolder item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    Set<String> saved = new HashSet<>(getStyleClass());
                    if (item.isAllocated()) {
                        saved.add("table-row-allocated-transaction");
                    } else {
                        saved.remove("table-row-allocated-transaction");
                    }
                    getStyleClass().clear();
                    getStyleClass().addAll(saved);
                }
            }
        };
    }

    private StringConverter<Account> getAccountStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Account account) {
                return Account.toKeyValue(account).key();
            }

            @Override
            public Account fromString(String string) {
                return null;
            }
        };
    }

    private ListCell<Account> makeComboBoxAccountCell(ListView<Account> accountListView) {
        return new ListCell<>() {
            @Override
            protected void updateItem(Account item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setGraphic(null);
                } else {
                    setText(Account.toKeyValue(item).key());
                    SVGPath tag = new SVGPath();
                    tag.setContent(AccountTag.svgPathContent);
                    tag.setFill(Color.web(item.tagColor()));
                    HBox hbox = new HBox(5.0, tag);
                    setGraphic(hbox);
                }
            }
        };
    }

    private StringConverter<LocalDate> convertSearchDate() {
        return new StringConverter<>() {
            final DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

            @Override
            public String toString(LocalDate date) {
                if (date != null) {
                    return dateFormatter.format(date);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String string) {
                if (string != null && !string.isEmpty()) {
                    return LocalDate.parse(string, dateFormatter);
                } else {
                    return null;
                }
            }
        };
    }

    private void listenToAccountsChanges(ObservableValue<? extends Account> observable, Account oldValue, Account newValue) {
        if (newValue != null && !newValue.equals(oldValue)) {
            loadStatementButton.setDisable(false);
            transactionModel.setAccount(newValue);
            settings.setCurrentAccount(Account.toKeyValue(newValue));
            disableIfNoAccountIsCurrent(false);
        }
    }

    private void listenToShowCompletedAlsoChanges(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
        if (newValue != null && !newValue.equals(oldValue)) {
            transactionModel.setShowCompleted(newValue);
        }
    }

    private void clearAction() {
        transactionModel.clearSearch();
        searchCriteria.setText("");
        searchDatePicker.setValue(null);
        singleTransactionsSelected = false;
        multipleTransactionsSelected = false;
        clear.setDisable(true);
    }

    private Predicate<? super TransactionHolder> defaultPredicate(Set<Long> transactions) {
        boolean showCompleted = showCompletedAlso.isSelected();
        return (t -> {
            if (showCompleted)
                return transactions.contains(t.getTransaction().id());
            else
                return transactions.contains(t.getTransaction().id()) && !t.isAllocated();
        });
    }

    @FXML
    private void onAddTransaction() {
        CreateTransactionDlgController dialog = CreateTransactionDlgController
                .getInstance(CountaryApp.OWNER_WINDOW, accounts.getValue());
        Optional<Transaction> value = dialog.showAndWait();
        value.ifPresent(
                transaction -> {
                    try {
                        Transaction newTransaction = model.createTransaction(transaction);
                        TransactionHolder holder = new TransactionHolder(newTransaction);
                        listOfTransactions.add(holder);
                        holderLookup.put(newTransaction, holder);
                        tableView.refresh();
                    } catch (SQLException e) {
                        throw new RuntimeException("Unable to create transaction", e);
                    }
                }
        );
    }

    @FXML
    private void onLoadStatement() {
        ImportStatementDlgController dlg = ImportStatementDlgController.getInstance(CountaryApp.OWNER_WINDOW, accounts.getValue());
        Optional<KeyValue> value = dlg.showAndWait();
        value.ifPresent(
                keyValue -> {
                    ParsedStatement parsedStatement = parseStatement(keyValue.value(), StatementParsers.valueOf(keyValue.key()).parser());
                    if (parsedStatement.getAccountNumber() != null && !parsedStatement.getAccountNumber().equals(transactionModel.getAccount().number())) {
                        userInterface.showError(String.format("Account number %s in statement is wrong", parsedStatement.getAccountNumber()));
                    } else {
                        List<Integer> hashes = parsedStatement.getLines().stream().map(ParsedStatement.Line::hashCode).toList();
                        List<ParsedStatement.Line> noDups;
                        try {
                            Set<Integer> matchingHashCodes = new HashSet<>(model.getTransactionHashesMatchingHashCodes(hashes));
                            noDups = parsedStatement.getLines().stream().filter(line -> !matchingHashCodes.contains(line.hashCode())).toList();
                        } catch (SQLException e) {
                            throw new RuntimeException("Exception while looking for duplicate transactions", e);
                        }
                        if (noDups.size() == 0)
                            return;
                        Account account = accounts.getValue();
                        try {
                            model.saveTransactions(account, noDups);
                        } catch (SQLException e) {
                            throw new RuntimeException("Exception while saving transactions in database", e);
                        }
                        transactionModel.setAccount(account);
                        onStatementLoaded.accept(account);
                    }
                }
        );
    }

    private ParsedStatement parseStatement(String filePath, Class<? extends StatementParser> parserClass) {
        try {
            StatementParser parser = parserClass.getDeclaredConstructor().newInstance();
            return parser.parse(new File(filePath).toURI());
        } catch (IOException | InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException("Unable to parse statement: " + filePath, e);
        } catch (StatementParseException e) {
            throw e;
        }
    }

    @FXML
    public void onCreateAccount() {
        CreateAccountDlgController controller = CreateAccountDlgController.getInstance(CountaryApp.OWNER_WINDOW);
        Optional<Account> optional = controller.showAndWait();
        optional.ifPresent(account -> {
            try {
                account = model.createAccount(account);
                accountsList.add(account);
                if (accounts.getValue() == null) {
                    accounts.setValue(account);
                    disableIfNoAccountIsCurrent(false);

                }
            } catch (SQLException e) {
                throw new RuntimeException("Unable to create account", e);
            }
        });
    }

    private void monitorSelectionChanges(ObservableList<? extends TransactionHolder> c) {
        if (c.size() == 0) {
            singleTransactionsSelected = false;
            multipleTransactionsSelected = false;
            onTransactionSelected.accept(null);
            calculate.setDisable(true);
        } else if (c.size() == 1) {
            singleTransactionsSelected = true;
            multipleTransactionsSelected = false;
            onTransactionSelected.accept(tableView.getSelectionModel().getSelectedItem());
            calculate.setDisable(false);
        } else {
            singleTransactionsSelected = false;
            multipleTransactionsSelected = true;
            onTransactionSelected.accept(null);
            calculate.setDisable(false);
        }
        applyToToolbox();
    }

    public void onTransactionChanged(Transaction transaction) {
        holderLookup.computeIfAbsent(transaction, key -> {
            TransactionHolder holder = new TransactionHolder(transaction);
            listOfTransactions.add(holder);
            return holder;
        });
        tableView.refresh();
    }

    private void applyToToolbox() {
        if (budgetItemSelected == null) {
            manyToOne.setDisable(true);
            oneToMany.setDisable(true);
        }
        if (singleTransactionsSelected && budgetItemSelected != null) {
            oneToMany.setDisable(false);
            oneToMany.setVisible(true);
            manyToOne.setVisible(false);
        }
        if (multipleTransactionsSelected && budgetItemSelected != null) {
            manyToOne.setDisable(false);
            manyToOne.setVisible(true);
            oneToMany.setVisible(false);
        }
    }

    private void mapManyTransactionsToOneBudgetItem() {
        ObservableList<TransactionHolder> transactions = tableView.getSelectionModel().getSelectedItems();
        int count = transactions.size();
        if (count > 1) {
            BigDecimal firstOne = transactions.get(0).getAmount();
            boolean sameSign = transactions.stream()
                    .map(t -> t.getAmount().signum())
                    .allMatch(t -> t == firstOne.signum());
            if (!sameSign) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("All the transaction amounts must have the same sign");
                alert.setTitle("An error occurred");
                alert.showAndWait();
            } else {
                CreateMto1AllocationDlgController dialog = CreateMto1AllocationDlgController
                        .getInstance(CountaryApp.OWNER_WINDOW, transactions,
                                budgetItemSelected.getBudgetItem());
                dialog.showAndWait().ifPresent(allocations -> {
                    List<TransactionHolder> replacements = new ArrayList<>();
                    List<DatabaseOperation<Connection>> allocationOperations = allocations.stream()
                            .map(allocation -> (DatabaseOperation<Connection>)
                                    (Connection connection) -> model.createAllocation(connection,
                                            allocation.transaction(), allocation.budgetItem(),
                                            allocation.amount(), allocation.note())
                            ).toList();
                    List<DatabaseOperation<Connection>> operations = new ArrayList<>(allocationOperations);
                    operations.add((Connection connection) -> {
                        replacements.addAll(model.setAllocated(connection, transactions.stream().map(TransactionHolder::getTransaction).toList()));
                    });
                    try {
                        model.doInTransaction(operations.toArray(new DatabaseOperation[0]));
                        replacements.forEach(holder -> holderLookup.get(holder.getTransaction()).setTransaction(holder.getTransaction()));
                        tableView.getSelectionModel().clearSelection();
                        tableView.refresh();
                        onAllocation.accept(allocations.get(0).transaction().account(), budgetItemSelected.getBudgetItem());
                    } catch (SQLException e) {
                        DbUtils.handleException(userInterface, "transaction", e);
                    }
                });
            }
        } else if (count == 1) {
            userInterface.showError("One transaction selected where multiple expected");
        } else {
            userInterface.showError("No transaction selected");
        }
    }

    private void mapOneTransactionToManyBudgetItems() {
        ObservableList<TransactionHolder> transactions = tableView.getSelectionModel().getSelectedItems();
        int count = transactions.size();
        if (count > 1) {
            userInterface.showError("Multiple transactions selected where one is expected");
        } else if (count == 1) {
            TransactionHolder transactionHolder = tableView.getSelectionModel().getSelectedItem();
            List<Allocation> allocations = null;
            try {
                allocations = model.getAllocations(transactionHolder.getTransaction().id());
            } catch (SQLException e) {
                throw new RuntimeException("Unexpected error retrieving existing allocations for transaction", e);
            }
            BigDecimal totalAllocations = allocations.stream()
                    .map(Allocation::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal balanceToAllocate = transactionHolder.getTransaction().amount().subtract(totalAllocations);
            Create1toMAllocationDlgController dialog = Create1toMAllocationDlgController
                    .getInstance(CountaryApp.OWNER_WINDOW, transactionHolder.getTransaction(), balanceToAllocate,
                            budgetItemSelected.getBudgetItem(),
                            allocations.stream().map(AllocationHolder::new).toList());
            dialog.showAndWait().ifPresent(allocation -> {
                try {
                    model.doInTransaction((connection) -> {
                        model.createAllocation(connection, allocation.transaction(), allocation.budgetItem(),
                                allocation.amount(), allocation.note());
                    }, (connection) -> {
                        if (allocation.amount().compareTo(balanceToAllocate) == 0) {
                            TransactionHolder newTransactionHolder = model.setAllocated(connection, transactionHolder.getTransaction());
                            holderLookup.get(transactionHolder.getTransaction()).setTransaction(newTransactionHolder.getTransaction());
                            tableView.getSelectionModel().clearSelection();
                            tableView.refresh();
                        }
                    });
                    onAllocation.accept(allocation.transaction().account(), budgetItemSelected.getBudgetItem());
                } catch (SQLException e) {
                    DbUtils.handleException(userInterface, "transaction", e);
                }
            });
        } else {
            userInterface.showError("No transactions are selected");
        }
    }

    private void textBasedSearch(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        if (newValue != null && !newValue.equals(oldValue) && newValue.length() > 1) {
            BigDecimal value = null;
            try {
                value = ParseUtils.parseBigDecimal(newValue);
            } catch (Exception e) {
                // need to know if it is a valid amount
            }
            if (value == null)
                transactionModel.searchByDescription(newValue);
            else {
                transactionModel.searchByAmount(newValue);
            }
        } else {
            transactionModel.clearSearch();
        }
    }

    private void dateBasedSearch(ObservableValue<? extends LocalDate> observable, LocalDate oldValue, LocalDate newValue) {
        if (newValue != null && !newValue.equals(oldValue)) {
            transactionModel.searchByDate(newValue);
        } else {
            transactionModel.clearSearch();
        }
    }
}
