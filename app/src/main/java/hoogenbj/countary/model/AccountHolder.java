/*
 * Copyright (c) 2023. Johan Hoogenboezem
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

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AccountHolder {

    private SimpleObjectProperty<Account> accountProperty;
    private StringProperty nameProperty;
    private StringProperty numberProperty;
    private StringProperty branchProperty;
    private StringProperty bankProperty;
    public AccountHolder(Account account) {
        setAccount(account);
        setName(account.name());
        setNumber(account.number());
        setBank(account.bank());
        setBranch(account.branchCode());
    }

    private void setBranch(String branchCode) {
        branchProperty().set(branchCode);
    }

    private void setBank(String bank) {
        bankProperty().set(bank);
    }

    private void setNumber(String number) {
        numberProperty().set(number);
    }

    private void setName(String name) {
        nameProperty().set(name);
    }

    public StringProperty nameProperty() {
        if (nameProperty == null) {
            nameProperty = new SimpleStringProperty(this, "name");
        }
        return  nameProperty;
    }
    public StringProperty numberProperty() {
        if (numberProperty == null) {
            numberProperty = new SimpleStringProperty(this, "number");
        }
        return  numberProperty;
    }
    public StringProperty branchProperty() {
        if (branchProperty == null) {
            branchProperty = new SimpleStringProperty(this, "branch");
        }
        return  branchProperty;
    }
    public StringProperty bankProperty() {
        if (bankProperty == null) {
            bankProperty = new SimpleStringProperty(this, "bank");
        }
        return  bankProperty;
    }

    public Account getAccount() {
        return accountProperty().get();
    }

    public void setAccount(Account account) {
        accountProperty().set(account);
    }

    public ObjectProperty<Account> accountProperty() {
        if (accountProperty == null) {
            accountProperty = new SimpleObjectProperty<>(this, "account");
        }
        return accountProperty;
    }

    public Long getId() {
        return accountProperty().get().id();
    }

    public String getName() {
        return nameProperty().get();
    }
}
