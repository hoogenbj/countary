/*
 * Copyright (c) 2022-2023. Johan Hoogenboezem
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

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CategoryModel {
    private final Map<Category, CategoryHolder> categoryLookup = new HashMap<>();
    private final Map<Category, Set<Category>> childrenLookup = new HashMap<>();
    private final Map<Integer, Set<Category>> treeLevels = new HashMap<>();
    private final Map<Category, Set<BudgetItemHolder>> categoryItems;
    private final Set<Category> categories;
    private final BudgetHolder budgetHolder;

    public CategoryModel(BudgetHolder budgetHolder, Set<Category> categories, Map<Category, Set<BudgetItemHolder>> categoryItems) {
        this.categoryItems = categoryItems;
        this.categories = categories;
        this.budgetHolder = budgetHolder;
        // The categories the budget items belong to constitute the 0-level of the tree. If they have parents, then
        // those constitute level 1 of the tree.
        categoryItems.keySet()
                .forEach(category -> {
                    Set<Category> zero = treeLevels.computeIfAbsent(0, l -> new HashSet<>());
                    zero.add(category);
                    if (category.parent() != null) {
                        Set<Category> children = childrenLookup.computeIfAbsent(category.parent(), parent -> new HashSet<>());
                        children.add(category);
                        Set<Category> one = treeLevels.computeIfAbsent(1, l -> new HashSet<>());
                        one.add(category.parent());
                    }
                });
        // Find all remaining levels of the category tree.
        Integer level = 2;
        AtomicInteger count = new AtomicInteger();
        do {
            count.set(0);
            Set<Category> keySet = new HashSet<>(childrenLookup.keySet());
            keySet.forEach(category -> {
                if (category.parent() != null) {
                    Set<Category> children = childrenLookup.computeIfAbsent(category.parent(), parent -> new HashSet<>());
                    if (!children.contains(category)) {
                        count.getAndIncrement();
                        children.add(category);
                        Set<Category> lvl = treeLevels.computeIfAbsent(level, l -> new HashSet<>());
                        lvl.add(category.parent());
                    }
                }
            });
        } while (count.get() > 0);
        treeLevels.getOrDefault(0, new HashSet<>()).forEach(category -> {
            categoryItems.get(category).forEach((holder) -> {
                updatePlanned(holder);
                updateActual(holder);
            });
        });
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public void updateActual(BudgetItemHolder budgetItemHolder) {
        Set<BudgetItemHolder> items = categoryItems.get(budgetItemHolder.getCategory());
        BigDecimal totalActual = items.stream()
                .map(BudgetItemHolder::getActual)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Category category = budgetItemHolder.getBudgetItem().item().category();
        categoryLookup.computeIfAbsent(category, CategoryHolder::new).setActual(totalActual);
        updateTreeActual();
        List<Category> roots = categoryLookup.keySet().stream().filter(c -> c.parent() == null).toList();
        totalActual = BigDecimal.ZERO;
        for (Category root : roots) {
            String actual = categoryLookup.get(root).getActual();
            totalActual = totalActual.add(ParseUtils.parseBigDecimal(actual));
        }
        budgetHolder.balanceProperty().set(totalActual);
    }

    public void updatePlanned(BudgetItemHolder budgetItemHolder) {
        Category category = budgetItemHolder.getCategory();
        Set<BudgetItemHolder> items = categoryItems.get(category);
        BigDecimal totalPlanned = items.stream()
                .map(BudgetItemHolder::getPlanned)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        categoryLookup.computeIfAbsent(category, CategoryHolder::new).setPlanned(totalPlanned);
        updateTreePlanned();
    }

    private void updateTreePlanned() {
        Set<Category> set;
        int treeLevel = 1;
        do {
            set = treeLevels.computeIfAbsent(treeLevel++, key -> new HashSet<>());
            for (Category parent : set) {
                BigDecimal total = childrenLookup.get(parent).stream()
                        .map(category -> {
                            CategoryHolder holder = categoryLookup.computeIfAbsent(category, CategoryHolder::new);
                            if (holder.getPlanned() != null)
                                return ParseUtils.parseBigDecimal(holder.getPlanned());
                            else
                                return BigDecimal.ZERO;
                        })
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                categoryLookup.computeIfAbsent(parent, CategoryHolder::new).setPlanned(total);
            }

        } while (!set.isEmpty());
    }

    private void updateTreeActual() {
        Set<Category> set;
        int treeLevel = 1;
        do {
            set = treeLevels.computeIfAbsent(treeLevel++, key -> new HashSet<>());
            for (Category parent : set) {
                BigDecimal total = childrenLookup.get(parent).stream()
                        .map(category -> {
                            CategoryHolder holder = categoryLookup.computeIfAbsent(category, CategoryHolder::new);
                            if (holder.getActual() != null)
                                return ParseUtils.parseBigDecimal(holder.getActual());
                            else
                                return BigDecimal.ZERO;
                        })
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                categoryLookup.computeIfAbsent(parent, CategoryHolder::new).setActual(total);
            }

        } while (!set.isEmpty());
    }

    public Map<Category, CategoryHolder> getCategoryLookup() {
        return categoryLookup;
    }

    public BudgetHolder getBudgetHolder() {
        return budgetHolder;
    }
}
