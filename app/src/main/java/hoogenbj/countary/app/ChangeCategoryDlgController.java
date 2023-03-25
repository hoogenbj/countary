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

public class ChangeCategoryDlgController extends Dialog<Category> {

    @FXML
    private ButtonType okButtonType;
    @FXML
    private HBox treeContainer;
    private Button okButton;
    @Inject
    private DataModel model;
    private Kind kind;

    public static ChangeCategoryDlgController getInstance(Window owner, Kind kind, Category selectedCategory) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(ChangeCategoryDlgController.class.getResource("ChangeCategoryDlg.fxml"));
        ChangeCategoryDlgController controller;
        try {
            DialogPane dlgPane = loader.load();
            controller = loader.getController();
            controller.setDialogPane(dlgPane);
            controller.initOwner(owner);
            controller.setTitle("Change category");
            controller.kind = kind;
            controller.selectedCategory = selectedCategory;
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            controller.okButton.setDisable(true);
            controller.okButton.setDefaultButton(true);
            controller.initControls();
            ChangeCategoryDlgController finalController = controller;
            controller.setResultConverter(buttonType -> {
                if (!Objects.equals(ButtonBar.ButtonData.OK_DONE, buttonType.getButtonData())) {
                    return null;
                }
                return finalController.composeResult();
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to launch " + ChangeCategoryDlgController.class.getSimpleName(), e);
        }
        return controller;
    }

    private Category composeResult() {
        return selectedCategory;
    }

    private Category selectedCategory;

    private void initControls() {
        Category inputCategory = selectedCategory;
        CategoryLiteTreeView treeView = CategoryLiteTreeView.getInstance(model, kind, inputCategory, (category, isLeaf) -> {
            if (isLeaf)
                selectedCategory = category;
            else
                selectedCategory = null;
            checkInputs();
        });
        HBox.setHgrow(treeView, Priority.ALWAYS);
        treeContainer.getChildren().add(treeView);
    }

    private void checkInputs() {
        okButton.setDisable(selectedCategory == null);
    }
}
