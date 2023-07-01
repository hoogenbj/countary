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

import hoogenbj.countary.model.BudgetItemHolder;
import hoogenbj.countary.util.InputUtils;
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
import java.net.URL;
import java.util.*;

public class CloneBudgetDlgController extends Dialog<Map<String, Object>> implements Initializable {

    public static final String NAME = "name";
    public static final String TRANSFER_BALANCE = "transferBalance";
    public static final String BUDGET_ITEM = "budgetItem";
    public static final String COPY_ACTUAL_TO_PLANNED = "copyActualToPlanned";
    @FXML
    private CheckBox copyActualToPlanned;
    @FXML
    private CheckBox transferBalance;
    @FXML
    private ChoiceBox<BudgetItemHolder> toBudgetItem;
    @FXML
    private TextField name;
    @FXML
    private ButtonType okButtonType;

    private Button okButton;
    private List<BudgetItemHolder> budgetItems;

    public static CloneBudgetDlgController getInstance(Window owner, List<BudgetItemHolder> budgetItems) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(CreateBudgetDlgController.class.getResource("CloneBudgetDlg.fxml"));
        CloneBudgetDlgController controller = null;
        try {
            DialogPane dlgPane = loader.load();
            controller = loader.getController();
            controller.initOwner(owner);
            controller.setTitle("Copy a budget");
            controller.budgetItems = budgetItems;
            controller.setDialogPane(dlgPane);
            controller.initControls();
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            controller.okButton.setDisable(true);
            CloneBudgetDlgController finalController = controller;
            controller.setResultConverter(buttonType -> {
                if(!Objects.equals(ButtonBar.ButtonData.OK_DONE, buttonType.getButtonData())) {
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
        result.put(NAME, name.getText());
        result.put(TRANSFER_BALANCE, transferBalance.isSelected());
        if (transferBalance.isSelected()) {
            result.put(BUDGET_ITEM, toBudgetItem.getValue());
        }
        result.put(COPY_ACTUAL_TO_PLANNED, copyActualToPlanned.isSelected());
        return result;
    }

    private enum Inputs {
        Name, CopyActual, TransferBalance, BudgetItem
    }

    private final EnumSet<Inputs> inputState = EnumSet.noneOf(Inputs.class);

    private void initControls() {
        ObservableList<BudgetItemHolder> list = FXCollections.observableList(budgetItems);
        SortedList<BudgetItemHolder> sortedList = new SortedList<>(list, Comparator.comparing(BudgetItemHolder::getName));
        toBudgetItem.setItems(sortedList);
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
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Runnable forAll = () -> okButton.setDisable(!inputState.containsAll(EnumSet.allOf(Inputs.class)));
        EnumSet<Inputs> some = EnumSet.of(Inputs.CopyActual, Inputs.TransferBalance, Inputs.Name);
        Runnable forAllExceptBudgetItem = () -> okButton.setDisable(!inputState.containsAll(some));
        InputUtils inputUtils = new InputUtils(forAll);
        inputUtils.observeChangesInInput(name.textProperty(), inputState, Inputs.Name, (string) -> !string.isEmpty());
        inputState.add(Inputs.CopyActual);
        inputUtils.observeChangesInInput(copyActualToPlanned.selectedProperty(), inputState, Inputs.CopyActual);
        inputUtils.observeChangesInInput(toBudgetItem.valueProperty(), inputState, Inputs.BudgetItem);
        inputState.add(Inputs.TransferBalance);
        transferBalance.selectedProperty().addListener((observable, oldValue, newValue) -> {
            Runnable theCallback;
            if (newValue != null && !newValue.equals(oldValue) && newValue) {
                theCallback = forAll;
                toBudgetItem.setDisable(false);
            } else {
                toBudgetItem.setDisable(true);
                theCallback = forAllExceptBudgetItem;
            }
            inputUtils.setCallback(theCallback);
            theCallback.run();
        });
        Platform.runLater(() -> name.requestFocus());
    }
}
