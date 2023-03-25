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

import hoogenbj.countary.util.ParseUtils;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class SummaryModel {

    private record TransactionSummary(BigDecimal debits, BigDecimal credits, BigDecimal balance) { }

    private final DataModel dataModel;
    private final SummaryHolder holder;
    private final Map<Long, TransactionSummary> transactionsSummary = new HashMap<>();
    private final Map<Long, BigDecimal> budgetActuals = new HashMap<>();

    public SummaryModel(DataModel dataModel, SummaryHolder holder) {
        this.dataModel = dataModel;
        this.holder = holder;
        try {
            dataModel.getAccounts().forEach(this::updateForAccount);
            refreshForTransactions();
            dataModel.getBudgets().forEach(this::updateForBudget);
            refreshForBudgets();
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve accounts", e);
        }
    }

    private void refreshForBudgets() {
        BigDecimal unfunded = BigDecimal.ZERO;
        BigDecimal funded = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entry: budgetActuals.entrySet()) {
            if (entry.getValue().signum() == -1) {
                unfunded = unfunded.add(entry.getValue());
            } else {
                funded = funded.add(entry.getValue());
            }
        }
        BigDecimal budgetBalance = funded.add(unfunded);
        holder.setBudgetBalance(ParseUtils.formatBigDecimal(budgetBalance));
        holder.setBudgetFunded(ParseUtils.formatBigDecimal(funded));
        holder.setBudgetUnfunded(ParseUtils.formatBigDecimal(unfunded));
    }

    private void refreshForTransactions() {
        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        BigDecimal balance = BigDecimal.ZERO;
        for (Map.Entry<Long, TransactionSummary> entry: transactionsSummary.entrySet()) {
            debits = debits.add(entry.getValue().debits());
            credits = credits.add(entry.getValue().credits());
            balance = balance.add(entry.getValue().balance());
        }
        holder.setTransactionBalance(ParseUtils.formatBigDecimal(balance));
        holder.setTransactionCredits(ParseUtils.formatBigDecimal(credits));
        holder.setTransactionDebits(ParseUtils.formatBigDecimal(debits));
    }

    private void updateForBudget(Budget budget) {
        try {
            Long id = budget.id();
            BigDecimal actualForBudget = dataModel.getActualForBudget(budget);
            budgetActuals.put(id, actualForBudget);
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Could not retrieve actual for budget %s"+budget.name()), e);
        }
    }

    private void updateForAccount(Account account) {
        try {
            List<Transaction> transactions = dataModel.getTransactions(account, false);
            BigDecimal sumDebits = transactions.stream()
                    .map(Transaction::amount)
                    .filter(amount -> amount.signum() == -1)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal sumCredits = transactions.stream()
                    .map(Transaction::amount)
                    .filter(amount -> amount.signum() == 1)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal balance = sumDebits.add(sumCredits);
            transactionsSummary.put(account.id(), new TransactionSummary(sumDebits, sumCredits, balance));
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Could not retrieve transactions for account %s",
                    account.name()), e);
        }
    }

    public void update(Account account, Budget budget) {
        updateForAccount(account);
        refreshForTransactions();
        updateForBudget(budget);
        refreshForBudgets();
    }

    public void update(Account account) {
        updateForAccount(account);
        refreshForTransactions();
    }

}
