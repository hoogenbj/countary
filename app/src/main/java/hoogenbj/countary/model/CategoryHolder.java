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

package hoogenbj.countary.model;

import hoogenbj.countary.app.BigDecimalProperty;
import hoogenbj.countary.util.ParseUtils;
import javafx.beans.property.*;

import java.math.BigDecimal;

public class CategoryHolder {

    private SimpleObjectProperty<Category> categoryProperty;
    private StringProperty actual;
    private StringProperty planned;
    private BooleanProperty selected;

    public CategoryHolder() {
    }

    public CategoryHolder(Category category) {
        setCategory(category);
    }

    public SimpleObjectProperty<Category> categoryProperty() {
        if (categoryProperty == null) {
            categoryProperty = new SimpleObjectProperty<>(this, "category");
        }
        return categoryProperty;
    }

    public Category getCategory() {
        return this.categoryProperty().get();
    }

    public void setCategory(Category category) {
        this.categoryProperty().set(category);
    }

    public String getPlanned() {
        return plannedProperty().get();
    }

    public void setPlanned(String planned) {
        this.plannedProperty().set(planned);
    }

    public void setPlanned(BigDecimal planned) {
        setPlanned(ParseUtils.formatBigDecimal(planned));
    }

    public StringProperty plannedProperty() {
        if (planned == null) planned = new SimpleStringProperty(this, "planned");
        return planned;
    }

    public String getActual() {
        return actualProperty().get();
    }

    public void setActual(String actual) {
        this.actualProperty().set(actual);
    }

    public void setActual(BigDecimal actual) {
        setActual(ParseUtils.formatBigDecimal(actual));
    }

    public StringProperty actualProperty() {
        if (actual == null) actual = new SimpleStringProperty(this, "actual");
        return actual;
    }

    public BooleanProperty selectedProperty() {
        if (selected == null) selected = new SimpleBooleanProperty(this, "selected");
        return selected;
    }
}
