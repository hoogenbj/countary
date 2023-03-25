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

import hoogenbj.countary.util.ParseUtils;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Objects;

public class AllocationHolder {
    private StringProperty amount;
    private StringProperty budgetName;
    private StringProperty itemName;
    private StringProperty note;
    private StringProperty transactionDescription;
    private StringProperty postingDate;
    private SimpleObjectProperty<Allocation> allocationProperty;
    private Allocation allocation;
    private DateFormat format = DateFormat.getDateInstance();

    public AllocationHolder() {
    }

    public AllocationHolder(Allocation allocation) {
        this.allocation = allocation;
        setAllocation(allocation);
        setBudgetName(allocation.budgetItem().budget().name());
        setItemName(allocation.budgetItem().item().name());
        setAmount(ParseUtils.formatBigDecimal(allocation.amount()));
        setNote(allocation.note());
        setPostingDate(format.format(allocation.transaction().postingDate()));
        setTransactionDescription(allocation.transaction().description());
    }

    public String getNote() {
        return noteProperty().get();
    }

    public StringProperty noteProperty() {
        if (note == null) {
            note = new SimpleStringProperty(this, "note");
        }
        return note;
    }

    public void setNote(String note) {
        this.noteProperty().set(note);
    }

    private void setTransactionDescription(String description) {
        transactionDescriptionProperty().set(description);
    }

    public StringProperty transactionDescriptionProperty() {
        if (transactionDescription == null)
            transactionDescription = new SimpleStringProperty(this, "transactionDescription");
        return transactionDescription;
    }

    private void setPostingDate(String postingDate) {
        this.postingDateProperty().set(postingDate);
    }

    public StringProperty postingDateProperty() {
        if (postingDate == null) postingDate = new SimpleStringProperty(this, "postingDate");
        return postingDate;
    }

    public StringProperty itemNameProperty() {
        if (itemName == null)
            itemName = new SimpleStringProperty(this, "item");
        return itemName;
    }

    public String getItemName() {
        return itemNameProperty().get();
    }

    public void setItemName(String itemName) {
        this.itemNameProperty().set(itemName);
    }

    public StringProperty budgetNameProperty() {
        if (budgetName == null)
            budgetName = new SimpleStringProperty(this, "budget");
        return budgetName;
    }

    public String getBudgetName() {
        return budgetNameProperty().get();
    }

    public void setBudgetName(String budgetName) {
        this.budgetNameProperty().set(budgetName);
    }

    public StringProperty amountProperty() {
        if (amount == null) amount = new SimpleStringProperty(this, "amount");
        return amount;
    }

    public String getAmount() {
        return amountProperty().get();
    }

    public Date getPostingDate() {
        try {
            return format.parse(postingDateProperty().get());
        } catch (ParseException e) {
            throw new RuntimeException("Unable to parse posting date", e);
        }
    }

    public void setAmount(String amount) {
        this.amountProperty().set(amount);
    }

    public SimpleObjectProperty<Allocation> allocationProperty() {
        if (allocationProperty == null) {
            allocationProperty = new SimpleObjectProperty<>(this, "allocation");
        }
        return allocationProperty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AllocationHolder that = (AllocationHolder) o;
        return Objects.equals(allocation, that.allocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allocation);
    }

    public Allocation getAllocation() {
        return allocationProperty().get();
    }

    public void setAllocation(Allocation allocation) {
        allocationProperty().set(allocation);
    }
}
