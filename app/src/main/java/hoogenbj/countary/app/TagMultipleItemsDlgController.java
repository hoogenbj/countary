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

import hoogenbj.countary.model.Budget;
import hoogenbj.countary.model.DataModel;
import hoogenbj.countary.model.ItemHolder;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TagMultipleItemsDlgController extends Dialog<Set<ItemHolder>> {
    @FXML
    private TextArea itemsField;
    @FXML
    private Pane tagPane;
    @FXML
    private ButtonType okButtonType;

    private UserInterface ui;
    private DataModel dataModel;
    private Budget budget;
    private Set<ItemHolder> items;
    private Button okButton;

    public static TagMultipleItemsDlgController getInstance(UserInterface ui, DataModel dataModel, Set<ItemHolder> items) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(SelectItemDlg.class.getResource("TagMultipleItemsDlg.fxml"));
        TagMultipleItemsDlgController controller = new TagMultipleItemsDlgController();
        controller.ui = ui;
        controller.dataModel = dataModel;
        controller.items = items;
        controller.initOwner(CountaryApp.OWNER_WINDOW);
        controller.setTitle("Tags for multiple items");
        loader.setController(controller);
        try {
            DialogPane dlgPane = loader.load();
            controller.setDialogPane(dlgPane);
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            controller.okButton.setDefaultButton(false);
            TagMultipleItemsDlgController finalController = controller;
            controller.setResultConverter(buttonType -> {
                if (!Objects.equals(ButtonBar.ButtonData.OK_DONE, buttonType.getButtonData())) {
                    return null;
                }
                return finalController.composeResult();
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to launch CreateBudgetItem Dialog", e);
        }
        return controller;
    }

    private Set<ItemHolder> composeResult() {
        return items;
    }

    @FXML
    private void initialize() {
        itemsField.setText(String.join(", ", items.stream().map(ItemHolder::getName).toList()));
        tagPane.getChildren().add(TaggingControl.getInstance(ui, dataModel, items.stream()
                .map(ItemHolder::getItem).collect(Collectors.toSet())));
    }
}
