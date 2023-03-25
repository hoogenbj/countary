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

import hoogenbj.countary.model.Category;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.io.IOException;

public class CategoryControlLite extends StackPane {

    @FXML
    private HBox categoryContainer;

    @FXML
    private Text categoryName;

    private Category category;

    public CategoryControlLite() {
    }

    public static CategoryControlLite getInstance(Category category) {
        FXMLLoader fxmlLoader = new FXMLLoader(CategoryControlLite.class.getResource("CategoryControlLite.fxml"));
        CategoryControlLite root = new CategoryControlLite();
        root.category = category;
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(root);
        try {
            fxmlLoader.load();
            return root;
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }


    public void initialize() {
        categoryContainer.setBackground(new Background(new BackgroundFill(category.getBgColor(),
                new CornerRadii(4.0), Insets.EMPTY)));
        categoryName.setText(category.name());
    }
}
