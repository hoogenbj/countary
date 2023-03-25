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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import javafx.util.Callback;

public abstract class CategoryTreeTableCell extends TreeTableCell<CategoryHolder, CategoryHolder> {
    private CategoryControl categoryControl;

    public CategoryTreeTableCell() {
        categoryControl = getCategoryControl();
    }
    // END copied from DefaultTreeCell<T>

    @Override
    public void updateItem(CategoryHolder item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            categoryControl.setCategoryHolder(item);
            setGraphic(categoryControl);
            categoryControl.bind(item.selectedProperty());
        }
    }

    public abstract CategoryControl getCategoryControl();
}
