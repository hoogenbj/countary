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
import javafx.beans.property.*;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.BiFunction;

public class BudgetItemHolder {
    private Long id;
    private StringProperty name;
    private BigDecimalProperty actual;
    private StringProperty note;
    private StringProperty kind;
    private BigDecimalProperty planned;
    private SimpleObjectProperty<BudgetItem> budgetItem;
    private SimpleObjectProperty<Category> categoryProperty;
    private BooleanProperty canDelete;

    public BudgetItemHolder() {
    }

    public BudgetItemHolder(BudgetItem budgetItem,
                            BiFunction<BudgetItem, BigDecimal, BudgetItem> onPlannedChange,
                            BiFunction<BudgetItem, String, BudgetItem> onNoteChange) {
        setBudgetItem(budgetItem);
        this.noteProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                budgetItemProperty().set(onNoteChange.apply(budgetItemProperty().get(), newValue));
            }
        });
        this.plannedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                budgetItemProperty().set(onPlannedChange.apply(budgetItemProperty().get(), newValue));
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BudgetItemHolder that = (BudgetItemHolder) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public StringProperty nameProperty() {
        if (name == null) name = new SimpleStringProperty(this, "name");
        return name;
    }

    public boolean isCanDelete() {
        return canDeleteProperty().get();
    }

    public BooleanProperty canDeleteProperty() {
        if (canDelete == null) {
            canDelete = new SimpleBooleanProperty(this, "canDelete");
        }
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDeleteProperty().set(canDelete);
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

    public StringProperty noteProperty() {
        if (note == null) note = new SimpleStringProperty(this, "note");
        return note;
    }

    public BigDecimalProperty plannedProperty() {
        if (planned == null) planned = new BigDecimalProperty(this, "planned");
        return planned;
    }

    public StringProperty kindProperty() {
        if (kind == null) kind = new SimpleStringProperty(this, "kind");
        return kind;
    }

    public SimpleObjectProperty<BudgetItem> budgetItemProperty() {
        if (budgetItem == null) budgetItem = new SimpleObjectProperty<>(this, "budgetItem");
        return budgetItem;
    }

    public BudgetItem getBudgetItem() {
        return budgetItemProperty().get();
    }

    public void setBudgetItem(BudgetItem budgetItem) {
        this.budgetItemProperty().set(budgetItem);
        setCanDelete(budgetItem.canDelete());
        setCategory(budgetItem.item().category());
        setName(budgetItem.item().name());
        setKind(budgetItem.budget().kind());
        setNote(budgetItem.note());
        setPlanned(budgetItem.planned());
        this.id = budgetItem.id();

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Kind getKind() {
        return Kind.valueOf(kindProperty().get());
    }

    public void setKind(Kind kind) {
        this.kindProperty().set(kind.name());
    }

    public String getNote() {
        return noteProperty().get();
    }

    public void setNote(String note) {
        this.noteProperty().set(note);
    }

    public String getName() {
        return nameProperty().get();
    }

    public void setName(String name) {
        this.nameProperty().set(name);
    }

    public BigDecimal getActual() {
        return actualProperty().get();
    }

    public void setActual(BigDecimal actual) {
        this.actualProperty().set(actual);
    }

    public BigDecimal getPlanned() {
        return plannedProperty().get();
    }

    public void setPlanned(BigDecimal planned) {
        this.plannedProperty().set(planned);
    }

    public BigDecimalProperty actualProperty() {
        if (actual == null) actual = new BigDecimalProperty(this, "actual");
        return actual;
    }
}
