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

import hoogenbj.countary.model.CategoryHolder;
import hoogenbj.countary.model.CategoryLiteHolder;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.io.IOException;

public class CategoryControl extends StackPane {

    @FXML
    private HBox categoryContainer;

    @FXML
    private Text categoryName;

    @FXML
    private CheckBox checkbox;

    private CategoryHolder categoryHolder;

    private Property<Boolean> boundProperty;

    public CategoryControl() {
    }

    public static CategoryControl getInstance() {
        FXMLLoader fxmlLoader = new FXMLLoader(CategoryControl.class.getResource("CategoryControl.fxml"));
        CategoryControl root = new CategoryControl();
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
    }

    public Property<Boolean> selectedProperty() {
        return checkbox.selectedProperty();
    }

    public Property<Boolean> indeterminateProperty() {
        return checkbox.indeterminateProperty();
    }

    public void setCategoryHolder(CategoryHolder categoryHolder) {
        this.categoryHolder = categoryHolder;
        categoryContainer.setBackground(new Background(new BackgroundFill(categoryHolder.getCategory().getBgColor(),
                new CornerRadii(4.0), Insets.EMPTY)));
        categoryName.setText(categoryHolder.getCategory().name());
    }

    public void bind(BooleanProperty selectedProperty) {
        if (boundProperty != null)
            checkbox.selectedProperty().unbindBidirectional(boundProperty);
        checkbox.selectedProperty().bindBidirectional(selectedProperty);
        boundProperty = selectedProperty;
    }
}
