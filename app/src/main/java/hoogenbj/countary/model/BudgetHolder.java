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
import hoogenbj.countary.app.KindProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.BiFunction;

public class BudgetHolder {
    private StringProperty name;
    private BigDecimalProperty balance;
    private KindProperty kindProperty;
    private BooleanProperty hiddenProperty;

    private Budget budget;

    public BudgetHolder() {
    }

    public BudgetHolder(Budget budget, BiFunction<Budget, Boolean, Budget> onHiddenChanged) {
        this.budget = budget;
        setName(budget.name());
        setKind(budget.kind());
        setHidden(budget.hidden());
        if (onHiddenChanged != null)
            this.hiddenProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && !newValue.equals(oldValue)) {
                    this.budget = onHiddenChanged.apply(budget, newValue);
                }
            });
    }

    public BigDecimal getBalance() {
        return balanceProperty().get();
    }

    public BigDecimalProperty balanceProperty() {
        if (balance == null) {
            balance = new BigDecimalProperty(this, "balance");
        }
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balanceProperty().set(balance);
    }

    public Budget getBudget() {
        return budget;
    }

    public Kind cat() {
        return budget.kind();
    }

    public String desc() {
        return budget.name();
    }

    public StringProperty nameProperty() {
        if (name == null) name = new SimpleStringProperty(this, "name");
        return name;
    }

    public KindProperty kindProperty() {
        if (kindProperty == null) kindProperty = new KindProperty(this, "kind");
        return kindProperty;
    }

    public boolean isHidden() {
        return hiddenProperty().get();
    }

    public BooleanProperty hiddenProperty() {
        if (hiddenProperty == null) {
            hiddenProperty = new SimpleBooleanProperty(this, "hidden");
        }
        return hiddenProperty;
    }

    public void setHidden(boolean hidden) {
        this.hiddenProperty().set(hidden);
    }

    public Long getId() {
        return budget.id();
    }

    public Kind getKind() {
        return kindProperty().get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BudgetHolder that = (BudgetHolder) o;
        return budget.equals(that.budget);
    }

    @Override
    public int hashCode() {
        return Objects.hash(budget);
    }

    public void setKind(Kind kind) {
        this.kindProperty().set(kind);
    }

    public String getName() {
        return nameProperty().get();
    }

    public void setName(String name) {
        this.nameProperty().set(name);
    }
}
