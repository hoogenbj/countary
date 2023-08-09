package hoogenbj.countary.app;

import hoogenbj.countary.model.Account;
import hoogenbj.countary.model.BudgetHolder;
import hoogenbj.countary.model.BudgetItemHolder;
import hoogenbj.countary.model.DataModel;
import hoogenbj.countary.util.InputUtils;
import hoogenbj.countary.util.ParseUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class TransferSingleAccountBudgetDlgController extends Dialog<Map<String, Object>>
        implements Initializable, ControllerHelpers {
    public static final String TRANSFER_AMOUNT = "transferAmount";
    public static final String FROM_BUDGET_ITEM = "fromBudgetItem";
    public static final String TO_BUDGET = "toBudget";
    public static final String TO_BUDGET_ITEM = "toBudgetItem";
    @FXML
    private ChoiceBox<BudgetItemHolder> fromBudgetItem;
    @FXML
    private ChoiceBox<BudgetHolder> toBudget;
    @FXML
    private TextField amount;
    @FXML
    private ChoiceBox<BudgetItemHolder> toBudgetItem;
    private Button okButton;
    @FXML
    private ButtonType okButtonType;
    private List<BudgetHolder> budgets;
    private DataModel dataModel;
    private BigDecimal balance;
    private Account account;
    private List<BudgetItemHolder> budgetItems;

    public static TransferSingleAccountBudgetDlgController getInstance(Window owner, DataModel dataModel, Account account,
                                                                       BigDecimal balance,
                                                                       List<BudgetItemHolder> budgetItems,
                                                                       List<BudgetHolder> budgets) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(TransferSingleAccountBudgetDlgController.class.getResource("TransferSingleAccountBudgetDlg.fxml"));
        TransferSingleAccountBudgetDlgController controller = null;
        try {
            DialogPane dlgPane = loader.load();
            controller = loader.getController();
            controller.initOwner(owner);
            controller.setTitle("Transfer from a budget");
            controller.budgets = budgets;
            controller.dataModel = dataModel;
            controller.balance = balance;
            controller.account = account;
            controller.budgetItems = budgetItems;
            controller.setDialogPane(dlgPane);
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            controller.okButton.setDisable(true);
            controller.initControls();
            TransferSingleAccountBudgetDlgController finalController = controller;
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
        Map<Account, BigDecimal> amountMap = new HashMap<>();
        amountMap.put(account, ParseUtils.parseBigDecimal(amount.getText()));
        result.put(TO_BUDGET, toBudget.getValue());
        result.put(TRANSFER_AMOUNT, amountMap);
        result.put(FROM_BUDGET_ITEM, fromBudgetItem.getValue());
        result.put(TO_BUDGET_ITEM, toBudgetItem.getValue());
        return result;
    }

    private void initControls() {
        amount.setText(ParseUtils.formatBigDecimalSimple(balance));
        ObservableList<BudgetHolder> list = FXCollections.observableList(budgets);
        SortedList<BudgetHolder> sortedList = new SortedList<>(list, Comparator.comparing(BudgetHolder::getName));
        toBudget.setItems(sortedList);
        toBudget.setConverter(getBudgetStringConverter());
        ObservableList<BudgetItemHolder> budgetItemList = FXCollections.observableList(budgetItems);
        SortedList<BudgetItemHolder> sortedBudgetItemList = new SortedList<>(budgetItemList, Comparator.comparing(BudgetItemHolder::getName));
        fromBudgetItem.setItems(sortedBudgetItemList);
        fromBudgetItem.setConverter(getBudgetItemStringConverter());
    }

    private enum Inputs {
        Amount, Budget, FromBudgetItem, ToBudgetItem
    }

    private final EnumSet<Inputs> inputState = EnumSet.noneOf(Inputs.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Runnable forAll = () -> okButton.setDisable(!inputState.containsAll(EnumSet.allOf(Inputs.class)));
        InputUtils inputUtils = new InputUtils(forAll);
        inputUtils.observeChangesInInput(amount.textProperty(), inputState, Inputs.Amount, (string) -> {
            if (string != null && !string.isEmpty()) {
                BigDecimal newValue = ParseUtils.parseBigDecimal(amount.getText());
                return (newValue.signum() == balance.signum() && newValue.compareTo(balance) < 1);
            } else
                return false;
        });
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
        Platform.runLater(() -> amount.requestFocus());
    }
}
