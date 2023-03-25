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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

public class CategoryModelTest {
    private static long id = 1;
    private static final Budget budget = new Budget(1L, null, "Monthly", Kind.Monthly, false);

    @Test
    public void test() {
        Category expenses = new Category(id++, "expenses", "", Kind.Monthly, null);
        Category expected = new Category(id++, "expected", "", Kind.Monthly, expenses);
        Category adhoc = new Category(id++, "adhoc", "", Kind.Monthly, expenses);
        Category bills = new Category(id++, "bills", "", Kind.Monthly, expected);
        Category variable = new Category(id++, "variable", "", Kind.Monthly, expected);
        Category eatingOut = new Category(id++, "eatingOut", "", Kind.Monthly, adhoc);
        BudgetItemHolder insurance = makeBudgetItemHolder("Insurance", "2000.00", bills);
        BudgetItemHolder ratesAndTaxes = makeBudgetItemHolder("Municipality", "3000.00", bills);
        BudgetItemHolder groceries = makeBudgetItemHolder("Groceries", "4500.00", variable);
        BudgetItemHolder fuel = makeBudgetItemHolder("Fuel", "1500.00", variable);
        BudgetItemHolder chinese = makeBudgetItemHolder("Chinese", "200.00", eatingOut);
        BudgetItemHolder italian = makeBudgetItemHolder("Italian", "400.00", eatingOut);
        BudgetItemHolder mexican = makeBudgetItemHolder("Mexican", "150.00", eatingOut);
        Map<Category, Set<BudgetItemHolder>> categoryItems = new HashMap<>();
        categoryItems.put(bills, Set.of(insurance, ratesAndTaxes));
        categoryItems.put(variable, Set.of(groceries, fuel));
        categoryItems.put(eatingOut, Set.of(chinese, italian, mexican));
        CategoryModel model = new CategoryModel(new BudgetHolder(budget, null), new HashSet<>(), categoryItems);
        Assertions.assertEquals(6, model.getCategoryLookup().size());
        model.getCategoryLookup().forEach((key, value) -> {
            System.out.printf("1 Category: %s total planned: %s\n", key.name(), value.getPlanned());
            switch (key.name()) {
                case "expenses" -> Assertions.assertEquals("11,750.00", value.getPlanned());
                case "expected" -> Assertions.assertEquals("11,000.00", value.getPlanned());
                case "adhoc" -> Assertions.assertEquals("750.00", value.getPlanned());
                case "bills" -> Assertions.assertEquals("5,000.00", value.getPlanned());
                case "variable" -> Assertions.assertEquals("6,000.00", value.getPlanned());
                case "eatingOut" -> Assertions.assertEquals("750.00", value.getPlanned());
            }
        });
        chinese.setPlanned(new BigDecimal("300.00"));
        model.updatePlanned(chinese);
        model.getCategoryLookup().forEach((key, value) -> {
            System.out.printf("2 Category: %s total planned: %s\n", key.name(), value.getPlanned());
            switch (key.name()) {
                case "expenses" -> Assertions.assertEquals("11,850.00", value.getPlanned());
                case "expected" -> Assertions.assertEquals("11,000.00", value.getPlanned());
                case "adhoc" -> Assertions.assertEquals("850.00", value.getPlanned());
                case "bills" -> Assertions.assertEquals("5,000.00", value.getPlanned());
                case "variable" -> Assertions.assertEquals("6,000.00", value.getPlanned());
                case "eatingOut" -> Assertions.assertEquals("850.00", value.getPlanned());
            }
        });
    }

    @Test
    public void testOneLevel() {
        Category bills = new Category(id++, "bills", "", Kind.Monthly, null);
        Category variable = new Category(id++, "variable", "", Kind.Monthly, null);
        Category eatingOut = new Category(id++, "eatingOut", "", Kind.Monthly, null);
        BudgetItemHolder insurance = makeBudgetItemHolder("Insurance", "2000.00", bills);
        BudgetItemHolder ratesAndTaxes = makeBudgetItemHolder("Municipality", "3000.00", bills);
        BudgetItemHolder groceries = makeBudgetItemHolder("Groceries", "4500.00", variable);
        BudgetItemHolder fuel = makeBudgetItemHolder("Fuel", "1500.00", variable);
        BudgetItemHolder chinese = makeBudgetItemHolder("Chinese", "200.00", eatingOut);
        BudgetItemHolder italian = makeBudgetItemHolder("Italian", "400.00", eatingOut);
        BudgetItemHolder mexican = makeBudgetItemHolder("Mexican", "150.00", eatingOut);
        Map<Category, Set<BudgetItemHolder>> categoryItems = new HashMap<>();
        categoryItems.put(bills, Set.of(insurance, ratesAndTaxes));
        categoryItems.put(variable, Set.of(groceries, fuel));
        categoryItems.put(eatingOut, Set.of(chinese, italian, mexican));
        CategoryModel model = new CategoryModel(new BudgetHolder(budget, null), new HashSet<>(), categoryItems);
        Assertions.assertEquals(3, model.getCategoryLookup().size());
        model.getCategoryLookup().forEach((key, value) -> {
            System.out.printf("1 Category: %s total planned: %s\n", key.name(), value.getPlanned());
            switch (key.name()) {
                case "bills" -> Assertions.assertEquals("5,000.00", value.getPlanned());
                case "variable" -> Assertions.assertEquals("6,000.00", value.getPlanned());
                case "eatingOut" -> Assertions.assertEquals("750.00", value.getPlanned());
            }
        });
        chinese.setPlanned(new BigDecimal("300.00"));
        model.updatePlanned(chinese);
        model.getCategoryLookup().forEach((key, value) -> {
            System.out.printf("2 Category: %s total planned: %s\n", key.name(), value.getPlanned());
            switch (key.name()) {
                case "bills" -> Assertions.assertEquals("5,000.00", value.getPlanned());
                case "variable" -> Assertions.assertEquals("6,000.00", value.getPlanned());
                case "eatingOut" -> Assertions.assertEquals("850.00", value.getPlanned());
            }
        });
    }

    private BudgetItemHolder makeBudgetItemHolder(String itemName, String planned, Category category) {
        Item item = new Item(id++, itemName, Kind.Monthly, category);
        BudgetItem budgetItem = new BudgetItem(id++, budget, item, "", planned, Collections.emptySet(), true);
        return new BudgetItemHolder(budgetItem, (one, two) -> one, (one, two) -> one);
    }
}
