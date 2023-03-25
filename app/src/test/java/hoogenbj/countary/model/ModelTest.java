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

import org.checkerframework.checker.units.qual.C;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static hoogenbj.countary.model.Kind.Monthly;
import static java.util.Set.of;

public class ModelTest {
    private final Tag incomeTag = new Tag(1L, "income");
    private final Tag expenseTag = new Tag(2L, "expense");
    private final Tag dayToDayTag = new Tag(3L, "daytoday");
    private final Tag plannedTag = new Tag(4L, "planned");
    private final Tag workerTag = new Tag(5L, "worker");
    private final Tag accountsTag = new Tag(6L, "accounts");
    private final Tag insuranceTag = new Tag(7L, "insurance");
    private final ItemTag income = new ItemTag(1L, null, incomeTag);
    private final ItemTag expense = new ItemTag(2L, null, expenseTag);

    @Test
    public void testOneTransactionManyItems() {
        Category income = new Category(1L, "Income", "", Monthly, null);
        Item salaryItem = new Item(1L, "salary", Monthly, income);
        BudgetItem salary = new BudgetItem(null, null, salaryItem, "", new BigDecimal("50000.00"), of(incomeTag), true);
        Category annualExpenses = new Category(2L, "Expenses", "", Kind.Annual, null);
        Item yearExpensesItem = new Item(2L, "year 2021", Kind.Annual, annualExpenses);
        BudgetItem yearExpenses = new BudgetItem(null, null, yearExpensesItem, "", new BigDecimal("250000.00"), of(expenseTag), true);
        Item holidaysItem = new Item(3L, "holidays 2021", Kind.Annual, annualExpenses);
        BudgetItem holidays = new BudgetItem(null, null, holidaysItem, "", new BigDecimal("15000.00"), of(expenseTag), true);
        Item pocketMoneyItem = new Item(4L, "pocketmoney 2021", Kind.Annual, annualExpenses);
        BudgetItem pocketMoney = new BudgetItem(null, null, pocketMoneyItem, "", new BigDecimal("6000.00"), of(expenseTag), true);
        Account sf = new Account(1L, "Single Facility", "123456789", "123456", "RMBPB", "tagColor");
        Transaction salaryTransaction = new Transaction(1L, sf, Date.from(Instant.parse("2021-12-03T10:15:30.00Z")),
                Date.from(Instant.parse("2021-12-03T10:15:30.00Z")),
                new BigDecimal("75000.00"), new BigDecimal("100000.00"), "Salary",
                1l, false, false, true);
        List<Allocation> allocations = List.of(
                new Allocation(salaryTransaction, yearExpenses, new BigDecimal("20833.33"), ""),
                new Allocation(salaryTransaction, pocketMoney, new BigDecimal("500.00"), ""),
                new Allocation(salaryTransaction, pocketMoney, new BigDecimal("500.00"), ""),
                new Allocation(salaryTransaction, holidays, new BigDecimal("1250.00"), "")
        );
        BigDecimal totalAllocations = allocations.stream().map(Allocation::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Allocation rest = new Allocation(salaryTransaction, salary, salaryTransaction.amount().subtract(totalAllocations), "");
        List<Allocation> all = new ArrayList<>(allocations);
        all.add(rest);
        Assertions.assertEquals(salaryTransaction.amount(), all.stream().map(Allocation::amount).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test
    public void testManyTransactionsOneItem() throws ParseException {
        Account sf = new Account(1L, "Single Facility", "123456789", "123456", "RMBPB", "");
        Transaction fuel1 = new Transaction(1L, sf, "2021/11/06", "2021/11/06", "-920.00", "0.0", "Fuel", null, false);
        Transaction fuel2 = new Transaction(5L, sf, "2021/11/20", "2021/11/20", "-846.00", "0.0", "Fuel", null, false);
        Category expenses = new Category(1L, "Expenses", "", Monthly, null);
        Item fuelItem = new Item(1L, "fuel", Kind.Monthly, expenses);
        BudgetItem fuel = new BudgetItem(null, null, fuelItem, "", "-1600.00", of(dayToDayTag, plannedTag, expenseTag), true);
        Allocation _1 = new Allocation(fuel1, fuel, fuel1.amount(), "");
        Allocation _2 = new Allocation(fuel2, fuel, fuel2.amount(), "");
        PlanActual planActual = new PlanActual(fuel.planned(), Stream.of(_1, _2).map(Allocation::amount).reduce(BigDecimal.ZERO, BigDecimal::add));
        Assertions.assertEquals(new BigDecimal("-1766.00"), planActual.actual());
    }

    @Test
    public void testMonthly() {
        Set<BudgetItem> budgetItems = getMonthBudget(new Budget(1L, null, "Dec 2021",
                Monthly, false));
        BigDecimal plannedSurplus = budgetItems.stream().map(BudgetItem::planned).reduce(BigDecimal.ZERO, BigDecimal::add);
        Assertions.assertEquals(new BigDecimal("30900.00"), plannedSurplus);
        Stream<TagAmountPair> tagStream = budgetItems.stream().<TagAmountPair>mapMulti((budgetItem, consumer) -> {
            for (Tag t : budgetItem.tags()) {
                consumer.accept(new TagAmountPair(t, budgetItem.planned()));
            }
        });
        Map<Tag, Metric> stats = new HashMap<>();
        tagStream.forEach(pair -> {
            Metric metric = stats.get(pair.tag());
            if (metric == null) {
                metric = new Metric();
                stats.put(pair.tag(), metric);
            }
            metric.inc();
            metric.add(pair.amount());
        });
        for (Map.Entry<Tag, Metric> entry : stats.entrySet()) {
            System.out.println("tag: " + entry.getKey().name() + " count=" + entry.getValue().getCount() + " sum=" + entry.getValue().getSum().toPlainString());
            switch (entry.getKey().name()) {
                case "income" -> {
                    Assertions.assertEquals(new BigDecimal("50000.00"), entry.getValue().getSum());
                    Assertions.assertEquals(1, entry.getValue().getCount());
                }
                case "daytoday" -> {
                    Assertions.assertEquals(new BigDecimal("-8100.00"), entry.getValue().getSum());
                    Assertions.assertEquals(5, entry.getValue().getCount());
                }
                case "expense", "planned" -> {
                    Assertions.assertEquals(new BigDecimal("-19100.00"), entry.getValue().getSum());
                    Assertions.assertEquals(9, entry.getValue().getCount());
                }
                case "insurance" -> {
                    Assertions.assertEquals(new BigDecimal("-3000.00"), entry.getValue().getSum());
                    Assertions.assertEquals(2, entry.getValue().getCount());
                }
                case "accounts" -> {
                    Assertions.assertEquals(new BigDecimal("-11000.00"), entry.getValue().getSum());
                    Assertions.assertEquals(4, entry.getValue().getCount());
                }
                case "worker" -> {
                    Assertions.assertEquals(new BigDecimal("-2200.00"), entry.getValue().getSum());
                    Assertions.assertEquals(2, entry.getValue().getCount());
                }
                default -> Assertions.fail("Unexpected tag: " + entry.getKey().name());
            }
        }
    }

    @Test
    public void testStatementRecon() throws Exception {
        List<Transaction> statement = getMonthStatement();
        Set<BudgetItem> budgetItems = getMonthBudget(new Budget(1L, null, "Dec 2021",
                Monthly, false));
    }

    private Set<BudgetItem> getMonthBudget(Budget budget) {
        Category income = new Category(1L, "income", "", Monthly, null);
        Category expenses = new Category(2L, "expense", "", Monthly, null);
        Item salaryItem = new Item(1L, "salary", Monthly, income);
        BudgetItem salary = new BudgetItem(1L, budget, salaryItem, "", "50000.00", of(incomeTag), true);
        Item janeItem = new Item(2L, "Jane", Kind.Monthly, expenses);
        BudgetItem jane = new BudgetItem(2L, budget, janeItem, "", "-1000.00", of(workerTag, dayToDayTag, plannedTag, expenseTag), true);
        Item johnItem = new Item(3L, "John", Kind.Monthly, expenses);
        BudgetItem john = new BudgetItem(3L, budget, johnItem, "", "-1200.00", of(workerTag, dayToDayTag, plannedTag, expenseTag), true);
        Item fuelItem = new Item(4L, "fuel", Kind.Monthly, expenses);
        BudgetItem fuel = new BudgetItem(4L, budget, fuelItem, "", "-1600.00", of(dayToDayTag, plannedTag, expenseTag), true);
        Item groceriesItem = new Item(5L, "groceries", Kind.Monthly, expenses);
        BudgetItem groceries = new BudgetItem(5L, budget, groceriesItem, "", "-4000.00", of(dayToDayTag, plannedTag, expenseTag), true);
        Item airtimeItem = new Item(6L, "airtime", Kind.Monthly, expenses);
        BudgetItem airtime = new BudgetItem(6L, budget, airtimeItem, "", "-300.00", of(dayToDayTag, plannedTag, expenseTag), true);
        Item lifeInsuranceItem = new Item(7L, "lifeInsurance", Kind.Monthly, expenses);
        BudgetItem lifeInsurance = new BudgetItem(7L, budget, lifeInsuranceItem, "", "-900.00", of(insuranceTag, accountsTag, plannedTag, expenseTag), true);
        Item shortTermInsuranceItem = new Item(8L, "shortTermInsurance", Kind.Monthly, expenses);
        BudgetItem shortTermInsurance = new BudgetItem(8L, budget, shortTermInsuranceItem, "", "-2100.00", of(insuranceTag, accountsTag, plannedTag, expenseTag), true);
        Item municipalItem = new Item(9L, "municipal", Kind.Monthly, expenses);
        BudgetItem municipal = new BudgetItem(9L, budget, municipalItem, "", "-3000.00", of(accountsTag, plannedTag, expenseTag), true);
        Item medicalAidItem = new Item(10L, "medicalAid", Kind.Monthly, expenses);
        BudgetItem medicalAid = new BudgetItem(10L, budget, medicalAidItem, "", "-5000.00", of(accountsTag, plannedTag, expenseTag), true);
        return Set.of(salary, jane, john, fuel, groceries, airtime, medicalAid, municipal, lifeInsurance, shortTermInsurance);
    }

    private List<Transaction> getMonthStatement() throws ParseException {
        Account sf = new Account(1L, "Single Facility", "123456789", "123456",
                "RMBPB", "tagColor");
        return List.of(
                new Transaction(1L, sf, "2021/11/06", "2021/11/06", "-920.00", "0.0", "Fuel", null, false),
                new Transaction(2L, sf, "2021/11/20", "2021/11/20", "-402.00", "0.0", "Groceries", null, false),
                new Transaction(3L, sf, "2021/11/18", "2021/11/18", "-1000.00", "0.0", "Wages for Jane", null, false),
                new Transaction(4L, sf, "2021/11/19", "2021/11/19", "-1200.00", "0.0", "Wages for John", null, false),
                new Transaction(5L, sf, "2021/11/20", "2021/11/20", "-846.00", "0.0", "Fuel", null, false),
                new Transaction(6L, sf, "2021/11/20", "2021/11/20", "-2706.00", "0.0", "Groceries", null, false),
                new Transaction(7L, sf, "2021/12/01", "2021/12/01", "-900.00", "0.0", "ACME Insurers", null, false),
                new Transaction(8L, sf, "2021/12/01", "2021/12/01", "-2100.00", "0.0", "Umbrella Car and House insurers", null, false),
                new Transaction(9L, sf, "2021/11/30", "2021/11/30", "-2976.00", "0.0", "Rates & Taxes", null, false),
                new Transaction(10L, sf, "2021/12/02", "2021/12/02", "-5000.00", "0.0", "Southern Med", null, false)
        );
    }
}
