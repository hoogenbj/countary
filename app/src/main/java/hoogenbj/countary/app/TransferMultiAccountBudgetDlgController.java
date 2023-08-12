package hoogenbj.countary.app;

import hoogenbj.countary.model.*;
import hoogenbj.countary.util.InputUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static hoogenbj.countary.app.TransferSingleAccountBudgetDlgController.*;

public class TransferMultiAccountBudgetDlgController extends Dialog<Map<String, Object>> implements Initializable, ControllerHelpers {
    @FXML
    private TableView<AccountBalanceHolder> tableView;
    @FXML
    private TableColumn<AccountBalanceHolder, String> accountColumn;
    @FXML
    private TableColumn<AccountBalanceHolder, BigDecimal> amountColumn;
    @FXML
    private ChoiceBox<BudgetItemHolder> fromBudgetItem;
    @FXML
    private ChoiceBox<BudgetHolder> toBudget;
    @FXML
    private ChoiceBox<BudgetItemHolder> toBudgetItem;
    private Button okButton;
    @FXML
    private ButtonType okButtonType;
    private List<BudgetHolder> budgets;
    private DataModel dataModel;
    private Map<Account, BigDecimal> balances;
    private List<BudgetItemHolder> budgetItems;

    public static TransferMultiAccountBudgetDlgController getInstance(Window owner, DataModel dataModel,
                                                                      Map<Account, BigDecimal> balances,
                                                                      List<BudgetItemHolder> budgetItems,
                                                                      List<BudgetHolder> budgets) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(TransferMultiAccountBudgetDlgController.class.getResource("TransferMultiAccountBudgetDlg.fxml"));
        TransferMultiAccountBudgetDlgController controller = null;
        try {
            DialogPane dlgPane = loader.load();
            controller = loader.getController();
            controller.initOwner(owner);
            controller.setTitle("Transfer from a budget");
            controller.budgets = budgets;
            controller.dataModel = dataModel;
            controller.balances = balances;
            controller.budgetItems = budgetItems;
            controller.setDialogPane(dlgPane);
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            controller.okButton.setDisable(true);
            controller.initControls();
            TransferMultiAccountBudgetDlgController finalController = controller;
            controller.setResultConverter(buttonType -> {
                if (!Objects.equals(ButtonBar.ButtonData.OK_DONE, buttonType.getButtonData())) {
                    return null;
                }
                return finalController.composeResult();
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to launch " + CloneBudgetDlgController.class.getSimpleName(), e);
        }
        return controller;
    }

    private Map<String, Object> composeResult() {
        Map<String, Object> result = new HashMap<>();
        result.put(TRANSFER_AMOUNT, tableView.getItems().stream()
                .collect(Collectors.toMap(AccountBalanceHolder::getAccount, AccountBalanceHolder::getAmount)));
        result.put(FROM_BUDGET_ITEM, fromBudgetItem.getValue());
        result.put(TO_BUDGET, toBudget.getValue());
        result.put(TO_BUDGET_ITEM, toBudgetItem.getValue());
        return result;
    }

    private enum Inputs {
        Budget, FromBudgetItem, ToBudgetItem
    }

    private final EnumSet<Inputs> inputState = EnumSet.noneOf(Inputs.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        amountColumn.setCellValueFactory(f -> f.getValue().amountProperty());
        amountColumn.setCellFactory(this::makeAmountCell);
        accountColumn.setCellValueFactory(f -> f.getValue().accountNameProperty());
        Runnable forAll = () -> okButton.setDisable(!inputState.containsAll(EnumSet.allOf(Inputs.class)));
        InputUtils inputUtils = new InputUtils(forAll);
        inputUtils.observeChangesInInput(fromBudgetItem.valueProperty(), inputState, Inputs.FromBudgetItem);
        inputUtils.observeChangesInInput(toBudget.valueProperty(), inputState, Inputs.Budget);
        inputUtils.observeChangesInInput(toBudgetItem.valueProperty(), inputState, Inputs.ToBudgetItem);
        toBudget.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                ObservableList<BudgetItemHolder> items;
                try {
                    items = FXCollections.observableList(new ArrayList<>(dataModel.getBudgetItemHolders(newValue.getBudget())));
                    SortedList<BudgetItemHolder> sortedItems = new SortedList<>(items, Comparator.comparing(BudgetItemHolder::getName));
                    toBudgetItem.setItems(sortedItems);
                    toBudgetItem.setConverter(new StringConverter<>() {
                        @Override
                        public String toString(BudgetItemHolder object) {
                            if (object != null) return object.getName();
                            else
                                return null;
                        }

                        @Override
                        public BudgetItemHolder fromString(String string) {
                            return null;
                        }
                    });
                } catch (SQLException e) {
                    throw new RuntimeException("Unable to retrieve budget items", e);
                }
            }
        });
        Platform.runLater(() -> tableView.requestFocus());
    }

    private TableCell<AccountBalanceHolder, BigDecimal> makeAmountCell(TableColumn<AccountBalanceHolder, BigDecimal> tableColumn) {
        TextFieldTableCell<AccountBalanceHolder, BigDecimal> cell = new TextFieldTableCell<>(){
            @Override
            public void updateItem(BigDecimal item, boolean empty) {
                if (item == null || empty) {
                    super.updateItem(item, empty) ;
                } else {
                    BigDecimal oldValue = getTableRow().getItem().getOriginalAmount();
                    if (item.compareTo(BigDecimal.ZERO) == 0 || (item.signum() == oldValue.signum() &&
                            item.abs().compareTo(oldValue.abs()) < 1)) {
                        super.updateItem(item, empty);
                    } else {
                        cancelEdit();
                    }
                }
            }
        };
        cell.setConverter(getDecimalStringConverter(cell));
        return cell;
    }

    private void initControls() {
        ObservableList<BudgetHolder> list = FXCollections.observableList(budgets);
        SortedList<BudgetHolder> sortedList = new SortedList<>(list, Comparator.comparing(BudgetHolder::getName));
        toBudget.setItems(sortedList);
        toBudget.setConverter(getBudgetStringConverter());
        ObservableList<BudgetItemHolder> budgetItemList = FXCollections.observableList(budgetItems);
        SortedList<BudgetItemHolder> sortedBudgetItemList = new SortedList<>(budgetItemList, Comparator.comparing(BudgetItemHolder::getName));
        fromBudgetItem.setItems(sortedBudgetItemList);
        fromBudgetItem.setConverter(getBudgetItemStringConverter());
        ObservableList<AccountBalanceHolder> balanceHolders = FXCollections.observableList(
                balances.entrySet().stream().map(entry -> new AccountBalanceHolder(entry.getKey(), entry.getValue())).toList());
        tableView.setItems(balanceHolders);
    }
}
