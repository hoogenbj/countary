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
import java.text.DateFormat;
import java.util.Date;
import java.util.Objects;

public class TransactionHolder {
    private Date pDate;
    private Date tDate;
    private StringProperty postingDate;
    private StringProperty txDate;
    private BigDecimalProperty amount;
    private BigDecimalProperty balance;
    private StringProperty description;
    private BooleanProperty canDelete;

    private SimpleObjectProperty<Transaction> transaction;

    public TransactionHolder() {
    }

    public TransactionHolder(Transaction transaction) {
        setTransaction(transaction);
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

    public SimpleObjectProperty<Transaction> transactionProperty() {
        if (transaction == null) {
            transaction = new SimpleObjectProperty<>(this, "transaction");
        }
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transactionProperty().set(transaction);
        setCanDelete(transaction.canDelete());
        DateFormat format = DateFormat.getDateInstance();
        setAmount(transaction.amount());
        setBalance(transaction.balance());
        setPostingDate(format.format(transaction.postingDate()));
        if (transaction.txdate() != null)
            setTxDate(format.format(transaction.txdate()));
        else
            setTxDate("");
        setDescription(transaction.description());

    }

    public Transaction getTransaction() {
        return transactionProperty().get();
    }

    public boolean isAllocated() {
        return getTransaction().allocated();
    }

    public Date pdate() {
        return getTransaction().postingDate();
    }

    public StringProperty postingDateProperty() {
        if (postingDate == null) postingDate = new SimpleStringProperty(this, "postingDate");
        return postingDate;
    }

    public StringProperty txDateProperty() {
        if (txDate == null) txDate = new SimpleStringProperty(this, "txDate");
        return txDate;
    }

    public BigDecimalProperty amountProperty() {
        if (amount == null) amount = new BigDecimalProperty(this, "amount");
        return amount;
    }

    public BigDecimalProperty balanceProperty() {
        if (balance == null) balance = new BigDecimalProperty(this, "balance");
        return balance;
    }

    public StringProperty descriptionProperty() {
        if (description == null) description = new SimpleStringProperty(this, "description");
        return description;
    }

    private void setPostingDate(String postingDate) {
        this.postingDateProperty().set(postingDate);
    }

    private void setTxDate(String txDate) {
        this.txDateProperty().set(txDate);
    }

    public BigDecimal getAmount() {
        return amountProperty().get();
    }

    private void setAmount(BigDecimal amount) {
        this.amountProperty().set(amount);
    }

    public BigDecimal getBalance() {
        return balanceProperty().get();
    }

    private void setBalance(BigDecimal balance) {
        this.balanceProperty().set(balance);
    }

    public String getDescription() {
        return descriptionProperty().get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionHolder that = (TransactionHolder) o;
        return transaction.get().equals(that.transaction.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(transaction);
    }

    private void setDescription(String description) {
        this.descriptionProperty().set(description);
    }
}
