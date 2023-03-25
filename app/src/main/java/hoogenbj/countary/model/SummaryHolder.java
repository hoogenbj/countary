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

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class SummaryHolder {
    private StringProperty budgetFunded = new SimpleStringProperty(this, "budgetFunded");
    private StringProperty budgetUnfunded = new SimpleStringProperty(this, "budgetUnfunded");
    private StringProperty budgetBalance = new SimpleStringProperty(this, "budgetBalance");
    private StringProperty transactionDebits = new SimpleStringProperty(this, "transactionDebits");
    private StringProperty transactionCredits = new SimpleStringProperty(this, "transactionCredits");
    private StringProperty transactionBalance = new SimpleStringProperty(this, "transactionBalance");

    public String getBudgetFunded() {
        return budgetFundedProperty().get();
    }

    public StringProperty budgetFundedProperty() {
        return budgetFunded;
    }

    public void setBudgetFunded(String budgetFunded) {
        this.budgetFundedProperty().set(budgetFunded);
    }

    public String getBudgetUnfunded() {
        return budgetUnfundedProperty().get();
    }

    public StringProperty budgetUnfundedProperty() {
        return budgetUnfunded;
    }

    public void setBudgetUnfunded(String budgetUnfunded) {
        this.budgetUnfundedProperty().set(budgetUnfunded);
    }

    public String getBudgetBalance() {
        return budgetBalanceProperty().get();
    }

    public StringProperty budgetBalanceProperty() {
        return budgetBalance;
    }

    public void setBudgetBalance(String budgetBalance) {
        this.budgetBalanceProperty().set(budgetBalance);
    }

    public String getTransactionDebits() {
        return transactionDebitsProperty().get();
    }

    public StringProperty transactionDebitsProperty() {
        return transactionDebits;
    }

    public void setTransactionDebits(String transactionDebits) {
        this.transactionDebitsProperty().set(transactionDebits);
    }

    public String getTransactionCredits() {
        return transactionCreditsProperty().get();
    }

    public StringProperty transactionCreditsProperty() {
        return transactionCredits;
    }

    public void setTransactionCredits(String transactionCredits) {
        this.transactionCreditsProperty().set(transactionCredits);
    }

    public String getTransactionBalance() {
        return transactionBalanceProperty().get();
    }

    public StringProperty transactionBalanceProperty() {
        return transactionBalance;
    }

    public void setTransactionBalance(String transactionBalance) {
        this.transactionBalanceProperty().set(transactionBalance);
    }
}
