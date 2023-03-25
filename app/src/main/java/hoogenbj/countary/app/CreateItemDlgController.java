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
import hoogenbj.countary.util.InputUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Window;

import java.io.IOException;
import java.net.URL;
import java.util.EnumSet;
import java.util.Objects;
import java.util.ResourceBundle;

public class CreateItemDlgController extends Dialog<Item> implements Initializable {

    @FXML
    private TextField name;
    @FXML
    private TextField note;
    @FXML
    private ChoiceBox<Kind> kindChoiceBox;
    @FXML
    private ButtonType okButtonType;
    @FXML
    private HBox treeContainer;
    private Button okButton;
    private DataModel model;

    public static CreateItemDlgController getInstance(Window owner, DataModel model) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(CreateItemDlgController.class.getResource("CreateItemDlg.fxml"));
        CreateItemDlgController controller = new CreateItemDlgController();
        controller.initOwner(owner);
        controller.setTitle("Create a item");
        controller.model = model;
        loader.setController(controller);
        try {
            DialogPane dlgPane = loader.load();
            controller = loader.getController();
            controller.setDialogPane(dlgPane);
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            controller.okButton.setDisable(true);
            controller.okButton.setDefaultButton(true);
            CreateItemDlgController finalController = controller;
            controller.setResultConverter(buttonType -> {
                if (!Objects.equals(ButtonBar.ButtonData.OK_DONE, buttonType.getButtonData())) {
                    return null;
                }
                return finalController.composeResult();
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to launch "+controller.getClass().getSimpleName(), e);
        }
        return controller;
    }

    private Item composeResult() {
        try {
            return new Item(null, name.getText(), kindChoiceBox.getValue(), selectedCategory);
        } catch (NumberFormatException e) {
            throw new RuntimeException("The value in Planned is not a number", e);
        }
    }

    private enum Inputs {
        Name, Kind
    }

    private Category selectedCategory;
    private final EnumSet<Inputs> inputState = EnumSet.noneOf(Inputs.class);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        kindChoiceBox.setItems(FXCollections.observableArrayList(Kind.Annual, Kind.Monthly, Kind.AdHoc));
        InputUtils<Inputs> inputUtils = new InputUtils<>(this::checkInputs);
        inputUtils.observeChangesInInput(name.textProperty(), inputState, Inputs.Name);
        inputUtils.observeChangesInInput(kindChoiceBox.valueProperty(), inputState, Inputs.Kind);
        kindChoiceBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                CategoryLiteTreeView treeView = CategoryLiteTreeView.getInstance(model, newValue, null, (category, isLeaf) -> {
                    if (isLeaf)
                        selectedCategory = category;
                    else
                        selectedCategory = null;
                    checkInputs();
                });
                treeContainer.getChildren().clear();
                HBox.setHgrow(treeView, Priority.ALWAYS);
                treeContainer.getChildren().add(treeView);
            }
        });
        Platform.runLater(() -> name.requestFocus());
    }

    private void checkInputs() {
        EnumSet<Inputs> all = EnumSet.allOf(Inputs.class);
        okButton.setDisable(!inputState.containsAll(all) || selectedCategory == null);
    }
}
