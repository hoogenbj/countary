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
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SelectItemDlg extends Dialog<List<Item>> implements Initializable {
    @FXML
    private TextField filterTextField;
    @FXML
    private ListView<ItemHolder> listView;
    @FXML
    private ButtonType okButtonType;

    private Button okButton;
    private Budget budget;
    private DataModel model;
    private FilteredList<ItemHolder> filteredList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        listView.getSelectionModel().selectedItemProperty().addListener(this::selected);
        listView.setCellFactory(this::makeListCell);
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        filterTextField.textProperty().addListener(this::changed);
        loadData();
    }

    private void loadData() {
        try {
            Set<Item> budgetItems = model.getBudgetItems(budget).stream().map(BudgetItem::item).collect(Collectors.toSet());
            ObservableList<ItemHolder> listOfItems =
                    FXCollections.observableArrayList(model.getItems().stream().filter(item -> !budgetItems.contains(item)).map(ItemHolder::new).toList());
            filteredList = new FilteredList<>(listOfItems);
            filteredList.setPredicate(getPredicate());
            SortedList<ItemHolder> sortedList = new SortedList<>(filteredList, Comparator.comparing(ItemHolder::desc));
            listView.setItems(sortedList);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load items", e);
        }
    }

    private ListCell<ItemHolder> makeListCell(ListView<ItemHolder> itemHolderListView) {
        return new ListCell<>() {
            @Override
            protected void updateItem(ItemHolder item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(null);
                if (!empty && item != null) {
                    setText(item.getName());
                }
            }
        };
    }

    private Predicate<ItemHolder> getPredicate() {
        return p -> p.cat().equals(budget.kind());
    }

    private Predicate<ItemHolder> getPredicate(Set<Long> ids) {
        return b -> b.cat().equals(budget.kind()) && ids.contains(b.getId());
    }

    private void doSearch(String criteria) {
        try {
            if (!criteria.contains("*"))
                criteria = String.format("%s*", criteria);
            Set<Long> items = model.searchItems("name", criteria);
            filteredList.setPredicate(getPredicate(items));
        } catch (SQLException e) {
            throw new RuntimeException("Unable to search items", e);
        }
    }

    public static SelectItemDlg getInstance(Window owner, DataModel model, Budget budget) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(SelectItemDlg.class.getResource("SelectItemDlg.fxml"));
        SelectItemDlg controller = new SelectItemDlg();
        controller.budget = budget;
        controller.model = model;
        controller.initOwner(owner);
        controller.setTitle("Select a item");
        loader.setController(controller);
        try {
            DialogPane dlgPane = loader.load();
            controller.setDialogPane(dlgPane);
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            controller.okButton.setDisable(true);
            SelectItemDlg finalController = controller;
            controller.setResultConverter(buttonType -> {
                if (!Objects.equals(ButtonBar.ButtonData.OK_DONE, buttonType.getButtonData())) {
                    return null;
                }
                return finalController.composeResult();
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to launch SelectItemDlg", e);
        }
        return controller;
    }

    private List<Item> composeResult() {
        return listView.getSelectionModel().getSelectedItems().stream().map(ItemHolder::getItem).toList();
    }

    private void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        if (newValue != null && newValue.length() >= 1 && !newValue.equals(oldValue)) {
            doSearch(newValue);
        } else {
            filteredList.setPredicate(getPredicate());
        }
    }

    private void selected(ObservableValue<? extends ItemHolder> observable, ItemHolder oldValue, ItemHolder newValue) {
        if (newValue != null && !newValue.equals(oldValue)) {
            okButton.setDisable(false);
        }
    }
}
