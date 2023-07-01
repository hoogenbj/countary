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

import hoogenbj.countary.app.KeyValue;
import hoogenbj.countary.app.UserInterface;
import hoogenbj.countary.util.ParsedStatement;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public interface DataModel {

    void clearCache();

    Map<Long, BigDecimal> getPlannedByBudgetAndTags(Budget budget) throws SQLException;

    Map<Long, BigDecimal> getActualByBudgetAndTags(Budget budget) throws SQLException;

    BigDecimal getActualByBudgetAndTag(Budget budget, Tag tag) throws SQLException;

    BigDecimal getActualForBudget(Budget budget) throws SQLException;

    BigDecimal getPlannedByBudgetAndTag(Budget budget, Tag tag) throws SQLException;

    Map<Long, Integer> getQuantifiedTagOrder() throws SQLException;

    boolean tableExists(String tableName) throws SQLException;

    List<KeyValue> getAccountsList() throws SQLException;

    Account getAccount(Long accountId) throws SQLException;

    List<Transaction> getTransactions(Account account, boolean showCompletedAlso) throws SQLException;

    Account createAccount(Account account) throws SQLException;

    List<Integer> getTransactionHashesMatchingHashCodes(List<Integer> hashes) throws SQLException;

    void saveTransactions(Account account, List<ParsedStatement.Line> lines) throws SQLException;

    Budget getBudget(Long id) throws SQLException;

    List<Budget> getBudgets() throws SQLException;

    Set<BudgetItem> getBudgetItems(Budget budget) throws SQLException;

    Set<BudgetItemHolder> getBudgetItemHolders(Budget budget,
                                               BiFunction<BudgetItem, BigDecimal, BudgetItem> onPlannedChange,
                                               BiFunction<BudgetItem, String, BudgetItem> onNoteChange) throws SQLException;

    Set<Long> searchBudgets(String what, String criteria) throws SQLException;

    Map<Account, BigDecimal> calculateBalances(Budget budget) throws SQLException;

    Budget createBudget(Budget budget) throws SQLException;

    Budget cloneBudget(Budget budget, String newName, Boolean copyActualToPlanned, Boolean transferBalance, BudgetItem budgetItem) throws SQLException;

    Item createItem(Item item) throws SQLException;

    Set<Long> searchItems(String what, String criteria) throws SQLException;

    List<Item> getItems() throws SQLException;

    List<TagHolder> getTags() throws SQLException;

    Tag getTag(String text) throws SQLException;

    Tag createTag(String text) throws SQLException;

    Allocation createAllocation(Connection connection, Transaction transaction, BudgetItem budgetItem,
                                BigDecimal amount, String note) throws SQLException;

    Set<Long> searchTags(String what, String criteria) throws SQLException;

    Set<Tag> getTagsForItem(Item item) throws SQLException;

    Set<ItemTag> getItemTags(Item item) throws SQLException;

    Collection<Set<Tag>> getItemTagsForBudget(Budget budget) throws SQLException;

    void deleteItemTag(ItemTag itemTag) throws SQLException;

    ItemTag createItemTag(Item item, Tag tag) throws SQLException;

    void createTagForItemIfNotExist(Item item, Tag tag) throws SQLException;

    BudgetItem addItemToBudget(Budget budget, Item item, BigDecimal planned, String note) throws SQLException;

    BudgetItem updateBudgetItemNote(BudgetItem budgetItem, String newValue) throws SQLException;

    BudgetItem updateBudgetItemPlanned(BudgetItem budgetItem, BigDecimal newValue) throws SQLException;

    Item updateItemCategory(Item item, Category c) throws SQLException;

    Item updateItemName(Item item, String newValue) throws SQLException;

    void deleteTagForItem(Item item, Long id) throws SQLException;

    boolean canDeleteItem(Item item) throws SQLException;

    boolean canDeleteCategory(Category category) throws SQLException;

    void deleteItem(Item item) throws SQLException;

    List<Allocation> getAllocations(Long transactionId) throws SQLException;

    BigDecimal getActualForBudgetItem(BudgetItem budgetItem) throws SQLException;

    Transaction getTransaction(Long id) throws SQLException;

    TransactionHolder setAllocated(Connection connection, Transaction transaction) throws SQLException;

    List<TransactionHolder> setAllocated(Connection connection, List<Transaction> transactions) throws SQLException;

    void doInTransaction(DatabaseOperation... consumers) throws SQLException;

    void deleteAllocation(Allocation allocation) throws SQLException;

    List<Allocation> getAllocationsByBudgetItem(BudgetItem budgetItem) throws SQLException;

    List<Account> getAccounts() throws SQLException;

    Transaction createTransaction(Transaction transaction) throws SQLException;

    Category renameCategory(Category category, String name) throws SQLException;

    Category createCategory(String name, Kind kind, String bgColor, Category parent) throws SQLException;

    List<Category> getCategoryRoots(Kind kind) throws SQLException;

    List<Category> getCategoryChildren(Category item) throws SQLException;

    Category updateCategoryBgColor(Category category, String color) throws SQLException;

    Category addCategorySibling(Category sibling, String newSiblingName) throws SQLException;

    Category addCategoryChild(Category category, String name) throws SQLException;

    void deleteCategory(Category category) throws SQLException;

    Set<Category> getBudgetCategories(Budget budget) throws SQLException;

    Budget updateBudgetHidden(Budget budget, Boolean aBoolean) throws SQLException;

    Set<Long> searchBudgetItems(Budget budget, String criteria) throws SQLException;

    boolean canDeleteTransaction(Transaction transaction) throws SQLException;

    void deleteTransaction(Transaction transaction) throws SQLException;

    boolean canDeleteBudgetItem(BudgetItem budgetItem) throws SQLException;

    void deleteBudgetItem(BudgetItem budgetItem) throws SQLException;

    BudgetItem getBudgetItem(Long id) throws SQLException;

    List<Transaction> searchTransactions(Account account, boolean showCompleted, String what, String currentCriteria) throws SQLException;

    void backup(String filePath) throws SQLException;

    void createDemoDatabase(List<String> meta, List<String> data) throws SQLException;

    void restore(String filePath) throws SQLException;

    void executeStatements(List<String> statements) throws SQLException;

    Set<Long> searchAccounts(String criteria) throws SQLException;

    Account updateAccountTagColor(Account account, String color) throws SQLException;

    void rebuildVirtualTables(UserInterface userInterface) throws SQLException;
}
