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

import hoogenbj.countary.app.KeyValue;
import hoogenbj.countary.app.UserInterface;

import static hoogenbj.countary.util.DbUtils.MAX_TRANSACTION_ROWS;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;

public class TransactionModel {
    private final Consumer<List<Transaction>> refreshTransactions;
    private final Consumer<Boolean> searchClearable;

    public Account getAccount() {
        return account;
    }

    public enum SearchChoice {
        Description(new KeyValue("Description", "description")),
        Amount(new KeyValue("Amount", "amount")),
        TransactionDate(new KeyValue("Transacted On", "txdate")),
        PostedDate(new KeyValue("Posted On", "posting_date"));
        private final KeyValue keyValue;

        public KeyValue getKeyValue() {
            return keyValue;
        }

        SearchChoice(KeyValue keyValue) {
            this.keyValue = keyValue;
        }
    }

    private DataModel dataModel;
    private SearchChoice currentSearch;
    private String currentCriteria;

    private Account account;

    private boolean showCompleted;

    public TransactionModel(DataModel dataModel,
                            Consumer<List<Transaction>> refreshTransactions, Consumer<Boolean> searchClearable) {
        this.dataModel = dataModel;
        this.refreshTransactions = refreshTransactions;
        this.searchClearable = searchClearable;
    }

    public void searchByAmount(String criteria) {
        currentSearch = SearchChoice.Amount;
        currentCriteria = String.format("'\"%s\"'", criteria);
        doSearch();
    }

    public void searchByDate(LocalDate postedDate) {
        currentSearch = SearchChoice.PostedDate;
        currentCriteria = String.valueOf(postedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        doSearch();
    }

    public void searchByDescription(String criteria) {
        currentSearch = SearchChoice.Description;
        if (!criteria.contains("*"))
            // Ensures searching for partial word will also work
            currentCriteria = String.format("\"%s*\"", criteria);
        else
            currentCriteria = String.format("\"%s\"", criteria);
        doSearch();
    }

    private void doSearch() {
        if (currentSearch == null) {
            try {
                List<Transaction> transactions = dataModel.getTransactions(account, showCompleted);
                refreshTransactions.accept(transactions);
                searchClearable.accept(false);
            } catch (SQLException e) {
                throw new RuntimeException("Unable to search transactions", e);
            }
        } else {
            if (currentSearch.equals(SearchChoice.PostedDate)) {
                List<Transaction> transactions;
                try {
                    transactions = dataModel.searchTransactions(account, showCompleted, SearchChoice.PostedDate.getKeyValue().value(), currentCriteria);
                    transactions.addAll(dataModel.searchTransactions(account, showCompleted, SearchChoice.TransactionDate.getKeyValue().value(), currentCriteria));
                    refreshTransactions.accept(transactions);
                    searchClearable.accept(true);
                } catch (SQLException e) {
                    throw new RuntimeException("Unable to search transactions", e);
                }
            } else {
                String what = currentSearch.getKeyValue().value();
                try {
                    List<Transaction> transactions = dataModel.searchTransactions(account, showCompleted, what, currentCriteria);
                    refreshTransactions.accept(transactions);
                    searchClearable.accept(true);
                } catch (SQLException e) {
                    throw new RuntimeException("Unable to search transactions", e);
                }
            }
        }
    }

    public void setAccount(Account account) {
        this.account = account;
        doSearch();
    }

    public void setShowCompleted(boolean showCompleted) {
        this.showCompleted = showCompleted;
        doSearch();
    }

    public void clearSearch() {
        currentSearch = null;
        currentCriteria = null;
        doSearch();
    }

    public void refresh() {
        doSearch();
    }
}
