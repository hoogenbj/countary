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

import hoogenbj.countary.model.BudgetItem;
import hoogenbj.countary.model.DisplayTagHolder;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * A custom control for displaying the Tags linked to a Item.
 */
public class DisplayTagsControl extends StackPane {

    @FXML
    private GridPane tagGroup;

    private BudgetItem budgetItem;

    public static DisplayTagsControl getInstance(BudgetItem budgetItem) {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(DisplayTagsControl.class.getResource("DisplayTagsControl.fxml"));
        DisplayTagsControl root = new DisplayTagsControl();
        root.budgetItem = budgetItem;
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(root);
        try {
            return fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public DisplayTagsControl() {
    }

    public void initialize() {
        budgetItem.tags().forEach(tag -> {
            addToTagGroup(DisplayTagHolder.of(tag));
        });
    }

    private int tagCount = 0;

    /**
     * Add a TagControl to the display
     *
     * @param tagHolder
     */
    private void addToTagGroup(DisplayTagHolder tagHolder) {
        int rowCount = tagCount / 3;
        int columnCount = tagCount % 3;
        DisplayTagControl tagControl = DisplayTagControl.getInstance(tagHolder);
        tagControl.setUserData(tagHolder);
        tagGroup.add(tagControl, columnCount, rowCount);
        tagCount++;
    }
}
