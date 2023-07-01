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

import com.google.inject.Inject;
import hoogenbj.countary.app.KeyValue;
import hoogenbj.countary.app.Settings;
import hoogenbj.countary.app.UserInterface;
import hoogenbj.countary.util.ParsedStatement;

import static org.sqlite.SQLiteErrorCode.*;

import org.sqlite.SQLiteConfig;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.Date;
import java.util.function.BiFunction;

import static hoogenbj.countary.util.DbUtils.MAX_TRANSACTION_ROWS;

public class DatabaseDataModel implements DataModel {

    @Inject
    private Settings settings;

    private static final Map<Long, Category> categoryCache = new HashMap<>();

    private final Properties connectionProperties;

    public DatabaseDataModel() {
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        connectionProperties = config.toProperties();
    }

    @Override
    public void clearCache() {
        categoryCache.clear();
    }

    @Override
    public void backup(String filePath) throws SQLException {
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format("backup to %s", filePath));
        }
    }

    @Override
    public void executeStatements(List<String> statements) throws SQLException {
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties)) {
            connection.setAutoCommit(false);
            executeStatements(connection, statements);
            connection.setAutoCommit(true);
        }
    }

    @Override
    public void createDemoDatabase(List<String> meta, List<String> data) throws SQLException {
        // This creates the metadata and the data
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties)) {
            connection.setAutoCommit(false);
            executeStatements(connection, meta);
            executeStatements(connection, data);
            connection.setAutoCommit(true);
        }
    }

    private void executeStatements(Connection connection, List<String> statements) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statements.forEach(s -> {
                try {
                    statement.executeUpdate(s);
                } catch (SQLException e) {
                    throw new RuntimeException("Unable to execute statement: " + s, e);
                }
            });
        }
    }

    @Override
    public void restore(String filePath) throws SQLException {
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format("restore from %s", filePath));
        }
    }

    @Override
    public Map<Long, BigDecimal> getPlannedByBudgetAndTags(Budget budget) throws SQLException {
        String query = "select t.id,sum(planned) from budget_item bf " +
                "join item_tag ft on bf.itemId=ft.itemId " +
                "join tag t on t.id=ft.tagId " +
                "where budgetId=? group by t.id";
        Map<Long, BigDecimal> planned = new HashMap<>();
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, budget.id());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    planned.put(rs.getLong(1), rs.getBigDecimal(2));
                }
            }
        }
        return planned;
    }

    @Override
    public Map<Long, BigDecimal> getActualByBudgetAndTags(Budget budget) throws SQLException {
        String query = "select t.id,sum(a.amount) from budget_item bf " +
                "join item_tag ft on bf.itemId=ft.itemId " +
                "join tag t on t.id=ft.tagId " +
                "join allocation a on a.budgetItemId=bf.id " +
                "where budgetId=? group by t.id";
        Map<Long, BigDecimal> actual = new HashMap<>();
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, budget.id());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    actual.put(rs.getLong(1), rs.getBigDecimal(2));
                }
            }
        }
        return actual;
    }

    @Override
    public BigDecimal getActualByBudgetAndTag(Budget budget, Tag tag) throws SQLException {
        String query = "select sum(a.amount) from budget_item bf " +
                "join item_tag ft on bf.itemId=ft.itemId " +
                "join tag t on t.id=ft.tagId " +
                "join allocation a on a.budgetItemId=bf.id " +
                "where budgetId=? and t.id = ?";
        return getSumByBudgetAndTag(budget, tag, query);
    }

    @Override
    public BigDecimal getActualForBudget(Budget budget) throws SQLException {
        String query = "select sum(a.amount) from budget_item bf join allocation a on a.budgetItemId=bf.id where budgetId=?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, budget.id());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    BigDecimal sum = rs.getBigDecimal(1);
                    if (rs.wasNull())
                        return BigDecimal.ZERO;
                    else
                        return sum;
                } else {
                    return BigDecimal.ZERO;
                }
            }
        }
    }

    @Override
    public BigDecimal getPlannedByBudgetAndTag(Budget budget, Tag tag) throws SQLException {
        String query = "select sum(bf.planned) from budget_item bf " +
                "join item_tag ft on bf.itemId=ft.itemId " +
                "where bf.budgetId=? and ft.tagId=?";
        return getSumByBudgetAndTag(budget, tag, query);
    }

    private BigDecimal getSumByBudgetAndTag(Budget budget, Tag tag, String query) throws SQLException {
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, budget.id());
            statement.setLong(2, tag.id());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                } else {
                    return null;
                }
            }
        }
    }

    @Override
    public Map<Long, Integer> getQuantifiedTagOrder() throws SQLException {
        String query = "select tagId, count(tagId) as ct from item_tag group by tagId order by ct desc";
        Map<Long, Integer> quantifiedTags = new HashMap<>();
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(query)) {
                while (rs.next()) {
                    quantifiedTags.put(rs.getLong(1), rs.getInt(2));
                }
            }
        }
        return quantifiedTags;
    }

    @Override
    public boolean tableExists(String tableName) throws SQLException {
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties)) {
            DatabaseMetaData md = connection.getMetaData();
            ResultSet rs = md.getTables(null, null, tableName, null);
            int count = 0;
            while (rs.next())
                count++;
            return count > 0;
        }
    }

    @Override
    public List<KeyValue> getAccountsList() throws SQLException {
        return getAccounts().stream().map(Account::toKeyValue).toList();
    }

    @Override
    public List<Account> getAccounts() throws SQLException {
        List<Account> list = new ArrayList<>();
        String query = "select id, name, number, branchCode, bank, tagColor from account";
        SQLiteConfig config = new SQLiteConfig();
        config.enforceForeignKeys(true);
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(query)) {
                while (rs.next()) {
                    Account account = new Account(rs.getLong(1), rs.getString(2),
                            rs.getString(3), rs.getString(4), rs.getString(5),
                            rs.getString(6));
                    list.add(account);
                }
            }
        }
        return list;
    }

    @Override
    public Account getAccount(Long accountId) throws SQLException {
        Account account = null;
        String query = "select id, name, number, branchCode, bank, tagColor from account where id = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, accountId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    account = new Account(rs.getLong(1), rs.getString(2),
                            rs.getString(3), rs.getString(4),
                            rs.getString(5), rs.getString(6));
                }
            }
        }
        return account;
    }

    @Override
    public List<Transaction> searchTransactions(Account account, boolean showCompleted, String what, String currentCriteria) throws SQLException {
        List<Transaction> transactionList = new ArrayList<>();
        String subQuery = "(select count(*)>0 from allocation a where a.transactionId=t.id)";
        String where;
        if (showCompleted) {
            where = String.format("where t.accountId = ? and idx.%s match %s limit %d", what, currentCriteria, MAX_TRANSACTION_ROWS);
        } else {
            where = String.format("where t.accountId = ? and t.allocated = false and idx.%s match %s limit %d",
                    what, currentCriteria, MAX_TRANSACTION_ROWS);
        }
        String query = String.format("select t.id, t.posting_date, t.txdate, t.amount, t.balance, t.description, t.hash, " +
                "t.allocated, t.manual, %s " +
                "from transactions t join transactions_idx idx on idx.id = t.id %s", subQuery, where);
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, account.id());
            buildTransactionsResult(account, transactionList, statement);
        }
        return transactionList;
    }

    private void buildTransactionsResult(Account account, List<Transaction> transactionList, PreparedStatement statement) throws SQLException {
        try (ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                java.util.Date postingDate = new java.util.Date(rs.getLong(2));
                Long l = rs.getLong(3);
                java.util.Date txDate = null;
                if (rs.wasNull())
                    l = null;
                else
                    txDate = new java.util.Date(l);
                Transaction t = new Transaction(rs.getLong(1), account, postingDate,
                        txDate,
                        rs.getBigDecimal(4), rs.getBigDecimal(5), rs.getString(6),
                        rs.getLong(7), rs.getBoolean(8), rs.getBoolean(9),
                        !rs.getBoolean(10));
                transactionList.add(t);
            }
        }
    }

    @Override
    public List<Transaction> getTransactions(Account account, boolean showCompletedAlso) throws SQLException {
        List<Transaction> transactionList = new ArrayList<>();
        String subQuery = "(select count(*)>0 from allocation a where a.transactionId=t.id)";
        String query = null;
        if (showCompletedAlso)
            query = String.format("select id, posting_date, txdate, amount, balance, description, hash, allocated, manual, %s " +
                    "from transactions t where accountId = ? limit %d", subQuery, MAX_TRANSACTION_ROWS);
        else
            query = String.format("select id, posting_date, txdate, amount, balance, description, hash, allocated, manual, %s " +
                    "from transactions t where accountId = ? and allocated = false limit %d", subQuery, MAX_TRANSACTION_ROWS);
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, account.id());
            buildTransactionsResult(account, transactionList, statement);
        }
        return transactionList;
    }

    private Set<Long> searchByCriteria(String withWhat, String query) throws SQLException {
        Set<Long> longs = new HashSet<>();
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, withWhat);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Long id = rs.getLong(1);
                    longs.add(id);
                }
            }
        }
        return longs;
    }

    @Override
    public Account createAccount(Account account) throws SQLException {
        String query = "insert into account(name, number, branchCode, bank, tagColor) values (?,?,?,?,?)";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            connection.setAutoCommit(true);
            statement.setString(1, account.name());
            statement.setString(2, account.number());
            statement.setString(3, account.branchCode());
            statement.setString(4, account.bank());
            statement.setString(5, account.tagColor());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new Account(generatedKeys.getLong(1), account.name(), account.number(),
                            account.branchCode(), account.bank(), account.tagColor());
                } else {
                    throw new RuntimeException("Unable to get generated key");
                }
            }
        }
    }

    @Override
    public Transaction createTransaction(Transaction transaction) throws SQLException {
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties)) {
            connection.setAutoCommit(true);
            return createTransaction(connection, transaction);
        }
    }

    public Transaction createTransaction(Connection connection, Transaction transaction) throws SQLException {
        String query = "insert into transactions(posting_date, txdate, description, amount, balance, manual, allocated, " +
                "hash, accountId) values (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setDate(1, new java.sql.Date(transaction.postingDate().getTime()));
            if (transaction.txdate() == null)
                statement.setNull(2, Types.INTEGER);
            else
                statement.setDate(2, new java.sql.Date(transaction.txdate().getTime()));
            statement.setString(3, transaction.description());
            statement.setBigDecimal(4, transaction.amount());
            statement.setBigDecimal(5, transaction.balance());
            statement.setInt(6, (transaction.manual()) ? 1 : 0);
            statement.setInt(7, (transaction.allocated()) ? 1 : 0);
            statement.setLong(8, transaction.hash());
            statement.setLong(9, transaction.account().id());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new Transaction(generatedKeys.getLong(1), transaction.account(),
                            transaction.postingDate(), transaction.txdate(), transaction.amount(), transaction.balance(),
                            transaction.description(), transaction.hash(), transaction.allocated(), transaction.manual(),
                            transaction.canDelete());
                } else {
                    throw new RuntimeException("Unable to get generated key");
                }
            }
        }
    }

    @Override
    public Category renameCategory(Category category, String name) throws SQLException {
        String query = "update category set name = ? where id = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(true);
            statement.setString(1, name);
            statement.setLong(2, category.id());
            int updated = statement.executeUpdate();
            if (updated != 1)
                throw new RuntimeException(String.format("Expected row count of 1 after updating name. Row count was %d instead.", updated));
            return new Category(category.id(), name, category.bgColor(), category.kind(), category.parent());
        }
    }

    @Override
    public Category createCategory(String name, Kind kind, String bgColor, Category parent) throws SQLException {
        String query = "insert into category(name, kind, bgColor, parentId) values (?,?,?,?)";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            connection.setAutoCommit(true);
            statement.setString(1, name);
            statement.setString(2, kind.name());
            statement.setString(3, bgColor);
            if (parent == null)
                statement.setNull(4, Types.INTEGER);
            else
                statement.setLong(4, parent.id());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new Category(generatedKeys.getLong(1), name, bgColor, kind, parent);
                } else {
                    throw new RuntimeException("Unable to get generated key");
                }
            }
        }
    }

    @Override
    public List<Category> getCategoryRoots(Kind kind) throws SQLException {
        String query = "select id, name, bgColor from category where kind = ? and parentId is null";
        List<Category> list = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, kind.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Category category = new Category(resultSet.getLong(1), resultSet.getString(2),
                            resultSet.getString(3), kind, null);
                    list.add(category);
                }
            }
        }
        return list;
    }

    @Override
    public List<Category> getCategoryChildren(Category item) throws SQLException {
        String query = "select id, name, bgColor from category where parentId=?";
        List<Category> list = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, item.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Category category = new Category(resultSet.getLong(1), resultSet.getString(2),
                            resultSet.getString(3), item.kind(), null);
                    list.add(category);
                }
            }
        }
        return list;
    }

    @Override
    public Category updateCategoryBgColor(Category category, String color) throws SQLException {
        String query = "update category set bgColor = ? where id = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(true);
            statement.setString(1, color);
            statement.setLong(2, category.id());
            int updated = statement.executeUpdate();
            if (updated != 1)
                throw new RuntimeException(String.format("Expected row count of 1 after updating name. Row count was %d instead.", updated));
            return new Category(category.id(), category.name(), color, category.kind(), category.parent());
        }
    }

    @Override
    public Account updateAccountTagColor(Account account, String color) throws SQLException {
        String query = "update account set tagColor = ? where id = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(true);
            statement.setString(1, color);
            statement.setLong(2, account.id());
            int updated = statement.executeUpdate();
            if (updated != 1)
                throw new RuntimeException(String.format("Expected row count of 1 after updating name. Row count was %d instead.", updated));
            return new Account(account.id(), account.name(), account.number(), account.branchCode(), account.bank(), color);
        }
    }

    @Override
    public void rebuildVirtualTables(UserInterface userInterface) {
        Set<String> virtualTables = Set.of("account_idx", "budget_idx", "item_idx", "tag_idx", "transactions_idx");
        virtualTables.forEach(vt -> {
            try {
                if (isVirtualTableCorrupt(vt)) {
                    rebuildVirtualTable(vt);
                    userInterface.showNotification(String.format("Rebuilt virtual table %s", vt));
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void rebuildVirtualTable(String vt) throws SQLException {
        String query = String.format("insert into %s(%s) values('rebuild')", vt, vt);
        executeStatements(List.of(query));
    }

    private boolean isVirtualTableCorrupt(String vt) throws SQLException {
        String query = String.format("insert into %s(%s) values('integrity-check')", vt, vt);
        try {
            executeStatements(List.of(query));
        } catch (SQLException e) {
            if (e.getErrorCode() == SQLITE_CORRUPT_VTAB.code) {
                return true;
            } else
                throw e;
        }
        return false;
    }

    @Override
    public Category addCategorySibling(Category sibling, String newSiblingName) throws SQLException {
        return createCategory(newSiblingName, sibling.kind(), sibling.bgColor(), sibling.parent());
    }

    @Override
    public Category addCategoryChild(Category category, String name) throws SQLException {
        return createCategory(name, category.kind(), category.bgColor(), category);
    }

    @Override
    public void deleteCategory(Category category) throws SQLException {
        if (canDeleteCategory(category)) {
            try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties)) {
                try (PreparedStatement statement = connection.prepareStatement("delete from category where id = ?")) {
                    statement.setLong(1, category.id());
                    statement.executeUpdate();
                }
            }
        } else {
            throw new RuntimeException("Cannot delete category");
        }
    }

    @Override
    public Set<Category> getBudgetCategories(Budget budget) throws SQLException {
        String query = "select distinct c.id, c.parentId from budget_item bi join item i on i.id = bi.itemId " +
                "join category c on c.id = i.categoryId where bi.budgetId = ?";
        Set<Category> set = new HashSet<>();
        Set<Category> parents = new HashSet<>();
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, budget.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    set.add(getCategory(connection, resultSet.getLong(1)));
                    Long parent = resultSet.getLong(2);
                    if (!resultSet.wasNull()) {
                        parents.add(getCategory(connection, parent));
                    }
                }
                do {
                    set.addAll(parents);
                    parents = findParentCategories(parents, connection);
                } while (!parents.isEmpty());
            }
        }
        return set;
    }

    private Set<Category> findParentCategories(Set<Category> parents, Connection connection) throws SQLException {
        String parentIds = String.join(",", parents.stream()
                .filter(Objects::nonNull)
                .map(category -> String.valueOf(category.id())).toList());
        String query = "select parentId from Category where id in (" +
                parentIds +
                ")";
        Set<Category> set = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Long parentId = resultSet.getLong(1);
                    if (!resultSet.wasNull())
                        set.add(getCategory(connection, parentId));
                }
            }
        }
        return set;
    }

    @Override
    public List<Integer> getTransactionHashesMatchingHashCodes(List<Integer> hashes) throws SQLException {
        String query = "select hash from transactions where hash in (" +
                String.join(",", hashes.stream().map(String::valueOf).toList()) + ")";
        System.out.println("query = " + query);
        List<Integer> found = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    found.add(rs.getInt(1));
                }
            }
        }
        return found;
    }

    @Override
    public void saveTransactions(Account account, List<ParsedStatement.Line> lines) throws SQLException {
        String query = "insert into transactions(accountId, posting_date, txdate, amount, balance, description, hash, allocated) values(?,?,?,?,?,?,?,0)";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(false);
            for (ParsedStatement.Line line : lines) {
                statement.setLong(1, account.id());
                statement.setDate(2, new java.sql.Date(line.getPostedOn().getTimeInMillis()));
                if (line.getTransactionDate() == null)
                    statement.setNull(3, Types.INTEGER);
                else
                    statement.setDate(3, new java.sql.Date(line.getTransactionDate().getTimeInMillis()));
                statement.setBigDecimal(4, line.getAmount());
                statement.setBigDecimal(5, line.getBalance());
                statement.setString(6, line.getDescription());
                statement.setInt(7, line.hashCode());
                statement.addBatch();
                statement.clearParameters();
            }
            statement.executeBatch();
            // transaction gets committed now
            connection.setAutoCommit(true);
        }
    }

    @Override
    public Budget getBudget(Long id) throws SQLException {
        String query = "select id, copyBudgetId, name, kind, hidden from budget where id = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Budget(resultSet.getLong(1), resultSet.getLong(2), resultSet.getString(3),
                            Kind.valueOf(resultSet.getString(4)), resultSet.getBoolean(5));
                } else
                    return null;
            }
        }
    }

    private Category getCategory(Connection connection, Long id) throws SQLException {
        Category category = categoryCache.get(id);
        if (category != null)
            return category;
        String query = "select id, name, kind, bgColor, parentId from category where id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Long parentId = resultSet.getLong(5);
                    Category parent;
                    if (resultSet.wasNull())
                        parent = null;
                    else
                        parent = getCategory(connection, parentId);
                    category = new Category(resultSet.getLong(1), resultSet.getString(2),
                            resultSet.getString(4),
                            Kind.valueOf(resultSet.getString(3)), parent);
                    categoryCache.put(category.id(), category);
                } else {
                    throw new NullPointerException("Unable to retrieve a category with id " + id);
                }
            }
        }
        return category;
    }

    @Override
    public List<Budget> getBudgets() throws SQLException {
        List<Budget> list = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("select id, copyBudgetId, name, kind, hidden from budget")) {
                while (resultSet.next()) {
                    Budget budget = new Budget(resultSet.getLong(1), resultSet.getLong(2), resultSet.getString(3),
                            Kind.valueOf(resultSet.getString(4)), resultSet.getBoolean(5));
                    list.add(budget);
                }
            }
        }
        return list;
    }

    @Override
    public Set<BudgetItem> getBudgetItems(Budget budget) throws SQLException {
        Set<BudgetItem> set = new HashSet<>();
        String query = "select f.id, f.name, bf.note, bf.planned, f.kind, bf.id, f.categoryId, " +
                "(select count(*)>0 from allocation a where a.budgetItemId=bf.id) " +
                "from item f " +
                "join budget_item bf on bf.itemId = f.id where bf.budgetId = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, budget.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Item item = new Item(resultSet.getLong(1), resultSet.getString(2),
                            Kind.valueOf(resultSet.getString(5)), getCategory(connection, resultSet.getLong(7)));
                    Set<Tag> tags = getTagsForItem(item);
                    set.add(new BudgetItem(resultSet.getLong(6), budget, item, resultSet.getString(3),
                            resultSet.getBigDecimal(4), tags, !resultSet.getBoolean(8)));
                }
            }
        }
        return set;
    }

    @Override
    public BudgetItem getBudgetItem(Long id) throws SQLException {
        String query = "select f.id, f.name, bf.note, bf.planned, f.kind, bf.id, f.categoryId, bf.budgetId, " +
                "(select count(*)>0 from allocation a where a.budgetItemId=bf.id) " +
                "from item f " +
                "join budget_item bf on bf.itemId = f.id " +
                "where bf.id = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Item item = new Item(resultSet.getLong(1), resultSet.getString(2),
                            Kind.valueOf(resultSet.getString(5)), getCategory(connection, resultSet.getLong(7)));
                    Budget budget = getBudget(resultSet.getLong(8));
                    Set<Tag> tags = getTagsForItem(item);
                    return new BudgetItem(resultSet.getLong(6), budget, item, resultSet.getString(3),
                            resultSet.getBigDecimal(4), tags, !resultSet.getBoolean(9));
                }
            }
        }
        return null;
    }

    @Override
    public Set<BudgetItemHolder> getBudgetItemHolders(Budget budget,
                                                      BiFunction<BudgetItem, BigDecimal, BudgetItem> onPlannedChange,
                                                      BiFunction<BudgetItem, String, BudgetItem> onNoteChange) throws SQLException {
        Set<BudgetItemHolder> set = new HashSet<>();
        String query = "select f.id, f.name, bf.note, bf.planned, f.kind, bf.id, " +
                "(select sum(amount) from allocation a where a.budgetItemId=bf.id ), f.categoryId, " +
                "(select count(*)>0 from allocation a where a.budgetItemId=bf.id) " +
                "from item f join budget_item bf on bf.itemId = f.id where bf.budgetId =?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, budget.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Item item = new Item(resultSet.getLong(1), resultSet.getString(2),
                            Kind.valueOf(resultSet.getString(5)), getCategory(connection, resultSet.getLong(8)));
                    Set<Tag> tags = getTagsForItem(item);
                    BudgetItem budgetItem = new BudgetItem(resultSet.getLong(6), budget, item, resultSet.getString(3),
                            resultSet.getBigDecimal(4), tags, !resultSet.getBoolean(9));
                    BudgetItemHolder holder = new BudgetItemHolder(budgetItem, onPlannedChange, onNoteChange);
                    BigDecimal actual = resultSet.getBigDecimal(7);
                    if (!resultSet.wasNull())
                        holder.setActual(actual);
                    set.add(holder);
                }
            }
        }
        return set;
    }

    @Override
    public Set<Long> searchBudgets(String forWhat, String withWhat) throws SQLException {
        String query = String.format("select id from budget_idx where %s match ?", forWhat);
        return searchByCriteria(withWhat, query);
    }

    @Override
    public Set<Long> searchAccounts(String criteria) throws SQLException {
        String query = "select id from account_idx where name match ?";
        Set<Long> longs = new HashSet<>();
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, criteria);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Long id = rs.getLong(1);
                    longs.add(id);
                }
            }
        }
        return longs;
    }

    @Override
    public Set<Long> searchBudgetItems(Budget budget, String criteria) throws SQLException {
        String query = "select bi.id from item_idx idx " +
                "join budget_item bi on bi.itemId = idx.id and bi.budgetId = ? where idx.name match ?";
        Set<Long> longs = new HashSet<>();
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, budget.id());
            statement.setString(2, criteria);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Long id = rs.getLong(1);
                    longs.add(id);
                }
            }
        }
        return longs;
    }

    @Override
    public boolean canDeleteTransaction(Transaction transaction) throws SQLException {
        String query = "select count(*) as count from allocation where transactionId = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query);
        ) {
            statement.setLong(1, transaction.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("count") == 0;
            }
        }
    }

    @Override
    public boolean canDeleteBudgetItem(BudgetItem budgetItem) throws SQLException {
        String query = "select count(*) as count from allocation where budgetItemId = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query);
        ) {
            statement.setLong(1, budgetItem.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("count") == 0;
            }
        }
    }

    @Override
    public void deleteTransaction(Transaction transaction) throws SQLException {
        String query = "delete from transactions where id = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(true);
            statement.setLong(1, transaction.id());
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteBudgetItem(BudgetItem budgetItem) throws SQLException {
        String query = "delete from budget_item where id = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(true);
            statement.setLong(1, budgetItem.id());
            statement.executeUpdate();
        }
    }

    @Override
    public Budget cloneBudget(Budget budget, String newName, Boolean copyActualToPlanned, Boolean transferBalance, BudgetItem budgetItem) throws SQLException {
        if (!transferBalance)
            return cloneBudget(budget, newName, copyActualToPlanned);
        else {
            String query = "insert into budget(name, kind, copyBudgetId, hidden) values (?,?,?,?)";
            try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties)) {
                connection.setAutoCommit(true);
                Budget clone;
                try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, newName);
                    statement.setString(2, budget.kind().name());
                    statement.setLong(3, budget.id());
                    statement.setBoolean(4, budget.hidden());
                    statement.executeUpdate();
                    try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            clone = new Budget(generatedKeys.getLong(1), budget.id(), newName, budget.kind(), budget.hidden());
                        } else {
                            throw new RuntimeException("Unable to get generated key");
                        }
                    }
                }
                if (copyActualToPlanned) {
                    Set<BudgetItem> set = getBudgetItems(budget);
                    query = "insert into budget_item(budgetId,itemId,planned,note) " +
                            "values(?,?,?,?)";
                    try (PreparedStatement statement = connection.prepareStatement(query)) {
                        set.forEach(bi -> {
                            try {
                                statement.clearParameters();
                                BigDecimal actual = getActualForBudgetItem(bi);
                                if (actual == null)
                                    actual = BigDecimal.ZERO;
                                statement.setLong(1, clone.id());
                                statement.setLong(2, bi.item().id());
                                statement.setBigDecimal(3, actual);
                                statement.setString(4, bi.note());
                                statement.executeUpdate();
                            } catch (SQLException e) {
                                throw new RuntimeException("Unable to insert budget clone's budget items", e);
                            }
                        });
                    }
                } else {
                    query = String.format(
                            "insert into budget_item(budgetId,itemId,planned,note) select %d,itemId,planned,note from budget_item where budgetId=%d",
                            clone.id(), budget.id());
                    try (PreparedStatement statement = connection.prepareStatement(query)) {
                        statement.executeUpdate();
                    }
                }
                BudgetItem targetBudgetItem = getBudgetItem(connection, clone, budgetItem.item());
                Map<Account, BigDecimal> balances = calculateBalances(connection, budget);
                Calendar postedOn = Calendar.getInstance();
                postedOn.setTime(Date.from(Instant.now()));
                for (Map.Entry<Account, BigDecimal> entry : balances.entrySet()) {
                    String description = "Closing balance";
                    long transactionHash = Objects.hash(postedOn, description, entry.getValue().negate(), null,
                            BigDecimal.ZERO);
                    Transaction closingBalance = createTransaction(connection, new Transaction(null, entry.getKey(), postedOn.getTime(), null,
                            entry.getValue().negate(), BigDecimal.ZERO, "Closing balance", transactionHash,
                            true, true, false));
                    createAllocation(connection, closingBalance, budgetItem, entry.getValue().negate(), "Transfering to " + newName);
                    Transaction transferAmount = createTransaction(connection, new Transaction(null, entry.getKey(), postedOn.getTime(), null,
                            entry.getValue(), BigDecimal.ZERO, "Transfer from " + budget.name(), transactionHash,
                            true, true, false));
                    createAllocation(connection, transferAmount, targetBudgetItem, entry.getValue(), "Transfering from " + budget.name());
                }
                connection.setAutoCommit(true);
                return clone;
            }
        }
    }

    private BudgetItem getBudgetItem(Connection connection, Budget budget, Item item) throws SQLException {
        String query = "select f.id, f.name, bf.note, bf.planned, f.kind, bf.id, f.categoryId, bf.budgetId " +
                "from item f join budget_item bf on bf.itemId = f.id " +
                "where f.id = ? and bf.budgetId = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, item.id());
            statement.setLong(2, budget.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Set<Tag> tags = getTagsForItem(item);
                    return new BudgetItem(resultSet.getLong(6), budget, item, resultSet.getString(3),
                            resultSet.getBigDecimal(4), tags, false);
                } else
                    throw new RuntimeException(
                            String.format("Expected to find a budget_item for budget %s and item %s but found nothing.",
                                    budget.name(), item.name()));
            }
        }
    }

    @Override
    public Map<Account, BigDecimal> calculateBalances(Budget budget) throws SQLException {
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties)) {
            connection.setAutoCommit(true);
            return calculateBalances(connection, budget);
        }
    }

    private Map<Account, BigDecimal> calculateBalances(Connection connection, Budget budget) throws SQLException {
        Map<Account, BigDecimal> balances = new HashMap<>();
        String query = "select sum(a.amount), act.id, act.name, act.number, act.branchCode, act.bank, act.tagColor " +
                "from budget_item bf " +
                "join allocation a on a.budgetItemId=bf.id " +
                "join transactions t on a.transactionId=t.id " +
                "join account act on t.accountId = act.id " +
                "where budgetId=? group by act.id";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, budget.id());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    BigDecimal sum = rs.getBigDecimal(1);
                    BigDecimal balance;
                    if (rs.wasNull())
                        balance = BigDecimal.ZERO;
                    else
                        balance = sum;
                    Account account = new Account(rs.getLong(2), rs.getString(3),
                            rs.getString(4), rs.getString(5),
                            rs.getString(6), rs.getString(7));
                    balances.put(account, balance);
                }
            }
        }
        return balances;
    }

    private Budget cloneBudget(Budget budget, String newName, boolean copyActualToPlanned) throws SQLException {
        String query = "insert into budget(name, kind, copyBudgetId, hidden) values (?,?,?,?)";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties)) {
            connection.setAutoCommit(true);
            Budget clone;
            try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, newName);
                statement.setString(2, budget.kind().name());
                statement.setLong(3, budget.id());
                statement.setBoolean(4, budget.hidden());
                statement.executeUpdate();
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        clone = new Budget(generatedKeys.getLong(1), budget.id(), newName, budget.kind(), budget.hidden());
                    } else {
                        throw new RuntimeException("Unable to get generated key");
                    }
                }
            }
            if (copyActualToPlanned) {
                Set<BudgetItem> set = getBudgetItems(budget);
                query = "insert into budget_item(budgetId,itemId,planned,note) " +
                        "values(?,?,?,?)";
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    set.forEach(bi -> {
                        try {
                            statement.clearParameters();
                            BigDecimal actual = getActualForBudgetItem(bi);
                            if (actual == null)
                                actual = BigDecimal.ZERO;
                            statement.setLong(1, clone.id());
                            statement.setLong(2, bi.item().id());
                            statement.setBigDecimal(3, actual);
                            statement.setString(4, bi.note());
                            statement.executeUpdate();
                        } catch (SQLException e) {
                            throw new RuntimeException("Unable to insert budget clone's budget items", e);
                        }
                    });
                }
            } else {
                query = String.format(
                        "insert into budget_item(budgetId,itemId,planned,note) select %d,itemId,planned,note from budget_item where budgetId=%d",
                        clone.id(), budget.id());
                try (PreparedStatement statement = connection.prepareStatement(query)) {
                    statement.executeUpdate();
                }
            }
            connection.setAutoCommit(true);
            return clone;
        }
    }

    @Override
    public Budget createBudget(Budget budget) throws SQLException {
        String query = "insert into budget(name, kind, hidden) values (?,?,?)";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            connection.setAutoCommit(true);
            statement.setString(1, budget.name());
            statement.setString(2, budget.kind().name());
            statement.setBoolean(3, budget.hidden());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new Budget(generatedKeys.getLong(1), null, budget.name(), budget.kind(), budget.hidden());
                } else {
                    throw new RuntimeException("Unable to get generated key");
                }
            }
        }
    }

    @Override
    public Item createItem(Item item) throws SQLException {
        String query = "insert into item(name, kind, categoryId) values (?,?,?)";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            connection.setAutoCommit(true);
            statement.setString(1, item.name());
            statement.setString(2, item.kind().name());
            statement.setLong(3, item.category().id());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new Item(generatedKeys.getLong(1), item.name(), item.kind(), item.category());
                } else {
                    throw new RuntimeException("Unable to get generated key");
                }
            }
        }
    }

    @Override
    public Set<Long> searchItems(String forWhat, String withWhat) throws SQLException {
        String query = String.format("select id from item_idx where %s match ?", forWhat);
        return searchByCriteria(withWhat, query);
    }

    @Override
    public List<Item> getItems() throws SQLException {
        List<Item> list = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("select id, name, kind, categoryId from item")) {
                while (resultSet.next()) {
                    Item item = new Item(resultSet.getLong(1), resultSet.getString(2),
                            Kind.valueOf(resultSet.getString(3)), getCategory(connection, resultSet.getLong(4)));
                    list.add(item);
                }
            }
        }
        return list;
    }

    @Override
    public Set<ItemTag> getItemTags(Item item) throws SQLException {
        Set<ItemTag> set = new HashSet<>();
        String query = "select ft.id, t.id, t.name from tag t " +
                "join item_tag ft on ft.tagId = t.id where ft.itemId = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, item.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    set.add(new ItemTag(resultSet.getLong(1), item, new Tag(resultSet.getLong(2), resultSet.getString(3))));
                }
            }
        }
        return set;
    }

    @Override
    public Collection<Set<Tag>> getItemTagsForBudget(Budget budget) throws SQLException {
        String query = "select ft.id,ft.itemId,ft.tagId,t.name from item_tag ft " +
                "join tag t on t.id=ft.tagId " +
                "where itemId in ( select itemId from budget_item where budgetId=?)";
        Map<Long, Set<Tag>> itemTags = new HashMap<>(30);
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, budget.id());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Long itemId = rs.getLong(2);
                    Long tagId = rs.getLong(3);
                    String tagName = rs.getString(4);
                    Set<Tag> tags = itemTags.computeIfAbsent(itemId, k -> new HashSet<>(Set.of(new Tag(tagId, tagName))));
                    tags.add(new Tag(tagId, tagName));
                }
            }
        }
        return itemTags.values();
    }

    public Set<Tag> getTagsForItem(Item item) throws SQLException {
        Set<Tag> set = new HashSet<>();
        String query = "select ft.id, t.id, t.name from tag t " +
                "join item_tag ft on ft.tagId = t.id where ft.itemId = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, item.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    set.add(new Tag(resultSet.getLong(2), resultSet.getString(3)));
                }
            }
        }
        return set;
    }

    @Override
    public void deleteItemTag(ItemTag itemTag) throws SQLException {
        String query = "delete from item_tag where id = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(true);
            statement.setLong(1, itemTag.id());
            statement.executeUpdate();
        }
    }

    @Override
    public ItemTag createItemTag(Item item, Tag tag) throws SQLException {
        String query = "insert into item_tag(itemId, tagId) values (?,?)";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            connection.setAutoCommit(true);
            statement.setLong(1, item.id());
            statement.setLong(2, tag.id());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new ItemTag(generatedKeys.getLong(1), item, tag);
                } else {
                    throw new RuntimeException("Unable to get generated key");
                }
            }
        }
    }

    private boolean tagExistsForItem(Item item, Tag tag) throws SQLException {
        String query = "select count(*) as count from item_tag where itemId = ? and tagId = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query);
        ) {
            statement.setLong(1, item.id());
            statement.setLong(2, tag.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("count") > 0;
            }
        }
    }

    @Override
    public void createTagForItemIfNotExist(Item item, Tag tag) throws SQLException {
        if (!tagExistsForItem(item, tag)) {
            createItemTag(item, tag);
        }
    }

    public BudgetItem addItemToBudget(Budget budget, Item item, BigDecimal planned, String note) throws SQLException {
        String query = "insert into budget_item(budgetId, itemId, planned, note) values(?,?,?,?)";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            connection.setAutoCommit(true);
            statement.setLong(1, budget.id());
            statement.setLong(2, item.id());
            statement.setBigDecimal(3, planned);
            statement.setString(4, note);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Set<Tag> itemTags = getTagsForItem(item);
                    return new BudgetItem(generatedKeys.getLong(1), budget, item, note, planned, itemTags, true);
                } else {
                    throw new RuntimeException("Unable to get generated key");
                }
            }
        }
    }

    @Override
    public Budget updateBudgetHidden(Budget budget, Boolean aBoolean) throws SQLException {
        String query = "update budget set hidden = ? where id = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(true);
            statement.setBoolean(1, aBoolean);
            statement.setLong(2, budget.id());
            int updated = statement.executeUpdate();
            if (updated != 1)
                throw new RuntimeException(String.format("Expected row count of 1 after updating Budget.hidden. Row count was %d instead.", updated));
            return new Budget(budget.id(), budget.copyBudgetId(), budget.name(), budget.kind(), budget.hidden());
        }
    }

    @Override
    public BudgetItem updateBudgetItemNote(BudgetItem budgetItem, String newValue) throws SQLException {
        String query = "update budget_item set note = ? where id = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(true);
            statement.setString(1, newValue);
            statement.setLong(2, budgetItem.id());
            int updated = statement.executeUpdate();
            if (updated != 1)
                throw new RuntimeException(String.format("Expected row count of 1 after updating note. Row count was %d instead.", updated));
            return new BudgetItem(budgetItem.id(), budgetItem.budget(), budgetItem.item(), newValue,
                    budgetItem.planned(), budgetItem.tags(), budgetItem.canDelete());
        }
    }

    @Override
    public BudgetItem updateBudgetItemPlanned(BudgetItem budgetItem, BigDecimal newValue) throws SQLException {
        String query = "update budget_item set planned = ? where id = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(true);
            statement.setBigDecimal(1, newValue);
            statement.setLong(2, budgetItem.id());
            int updated = statement.executeUpdate();
            if (updated != 1)
                throw new RuntimeException(String.format("Expected row count of 1 after updating planned. Row count was %d instead.", updated));
            return new BudgetItem(budgetItem.id(), budgetItem.budget(), budgetItem.item(), budgetItem.note(), newValue,
                    budgetItem.tags(), budgetItem.canDelete());
        }
    }

    @Override
    public Item updateItemCategory(Item item, Category c) throws SQLException {
        String query = "update item set categoryId = ? where id = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(true);
            statement.setLong(1, c.id());
            statement.setLong(2, item.id());
            int updated = statement.executeUpdate();
            if (updated != 1)
                throw new RuntimeException(String.format("Expected row count of 1 after updating category. Row count was %d instead.", updated));
            return new Item(item.id(), item.name(), item.kind(), c);
        }
    }

    @Override
    public Item updateItemName(Item item, String newValue) throws SQLException {
        String query = "update item set name = ? where id = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(true);
            statement.setString(1, newValue);
            statement.setLong(2, item.id());
            int updated = statement.executeUpdate();
            if (updated != 1)
                throw new RuntimeException(String.format("Expected row count of 1 after updating name. Row count was %d instead.", updated));
            return new Item(item.id(), newValue, item.kind(), item.category());
        }
    }

    @Override
    public void deleteTagForItem(Item item, Long id) throws SQLException {
        String query = "delete from item_tag where itemId = ? and tagId = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            connection.setAutoCommit(true);
            statement.setLong(1, item.id());
            statement.setLong(2, id);
            if (statement.executeUpdate() != 1) {
                throw new RuntimeException(String.format("Couldn't delete tag with id %d from item with id %d", id, item.id()));
            }
        }
    }

    @Override
    public boolean canDeleteItem(Item item) throws SQLException {
        String query = "select count(*) as count from budget_item where itemId = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query);
        ) {
            statement.setLong(1, item.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("count") == 0;
            }
        }
    }

    @Override
    public boolean canDeleteCategory(Category category) throws SQLException {
        if (category.id() == null)
            return false;
        String query = "select count(*) as count from item where categoryId = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query);
        ) {
            statement.setLong(1, category.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                if (resultSet.getLong("count") > 0)
                    return false;
            }
        }
        query = "select count(*) as count from category where parentId = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query);
        ) {
            statement.setLong(1, category.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong("count") == 0;
            }
        }
    }

    @Override
    public void deleteItem(Item item) throws SQLException {
        if (canDeleteItem(item)) {
            try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties)) {
                connection.setAutoCommit(false);
                try (PreparedStatement statement = connection.prepareStatement("delete from item_tag where itemId = ?")) {
                    statement.setLong(1, item.id());
                    statement.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("delete from item where id = ?")) {
                    statement.setLong(1, item.id());
                    statement.executeUpdate();
                }
                connection.setAutoCommit(true);
            }
        } else {
            throw new RuntimeException("Cannot delete item");
        }
    }
    @Override
    public List<Allocation> getAllocations(Long transactionId) throws SQLException {
        List<Allocation> list = new ArrayList<>();
        String query = "select id, budgetItemId, amount, note from allocation where transactionId = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, transactionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    list.add(new Allocation(resultSet.getLong(1),
                            getTransaction(transactionId),
                            getBudgetItem(resultSet.getLong(2)),
                            resultSet.getBigDecimal(3), resultSet.getString(4)));
                }
            }
        }
        return list;
    }

    @Override
    public List<Allocation> getAllocationsByBudgetItem(BudgetItem budgetItem) throws SQLException {
        List<Allocation> list = new ArrayList<>();
        String query = "select id, transactionId, amount, note from allocation where budgetItemId = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, budgetItem.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    list.add(new Allocation(resultSet.getLong(1),
                            getTransaction(resultSet.getLong(2)),
                            budgetItem,
                            resultSet.getBigDecimal(3), resultSet.getString(4)));
                }
            }
        }
        return list;
    }

    @Override
    public BigDecimal getActualForBudgetItem(BudgetItem budgetItem) throws SQLException {
        String query = "select sum(amount) from allocation where budgetItemId = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, budgetItem.id());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBigDecimal(1);
                } else {
                    return null;
                }
            }
        }
    }

    public Transaction getTransaction(Long transactionId) throws SQLException {
        Transaction transaction = null;
        String query = "select posting_date, txdate, amount, balance, description, hash, allocated, accountId, manual," +
                "(select count(*)>0 from allocation a where a.transactionId=t.id) " +
                "from transactions t where id = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, transactionId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    java.util.Date postingDate = new java.util.Date(rs.getLong(1));
                    Long l = rs.getLong(2);
                    java.util.Date txDate = null;
                    if (rs.wasNull())
                        l = null;
                    else
                        txDate = new java.util.Date(l);
                    transaction = new Transaction(transactionId, getAccount(rs.getLong(8)), postingDate,
                            txDate,
                            rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getString(5),
                            rs.getLong(6), rs.getBoolean(7), rs.getBoolean(9),
                            !rs.getBoolean(10));
                }
            }
        }
        return transaction;
    }

    @Override
    public TransactionHolder setAllocated(Connection connection, Transaction transaction) throws SQLException {
        String query = "update transactions set allocated = true where id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, transaction.id());
            int updated = statement.executeUpdate();
            if (updated != 1)
                throw new RuntimeException(String.format("Expected row count of 1 after updating planned. Row count was %d instead.", updated));
            return new TransactionHolder(transaction.addAllocated(true));
        }
    }

    @Override
    public List<TransactionHolder> setAllocated(Connection connection, List<Transaction> transactions) throws SQLException {
        String query = "update transactions set allocated = true where id = ?";
        List<TransactionHolder> holderList = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            for (Transaction transaction : transactions) {
                statement.setLong(1, transaction.id());
                statement.addBatch();
                statement.clearParameters();
                holderList.add(new TransactionHolder(transaction.addAllocated(true)));
            }
            statement.executeBatch();
        }
        return holderList;
    }

    @Override
    public List<TagHolder> getTags() throws SQLException {
        List<TagHolder> list = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("select id, name from tag")) {
                while (resultSet.next()) {
                    TagHolder tag = new TagHolder(resultSet.getString(2), resultSet.getLong(1));
                    list.add(tag);
                }
            }
        }
        return list;
    }

    @Override
    public Tag getTag(String text) throws SQLException {
        Tag tag = null;
        String query = "select id, name from tag where name = ?";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, text.toLowerCase());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    tag = new Tag(resultSet.getLong(1), resultSet.getString(2));
                }
            }
        }
        return tag;
    }

    @Override
    public Tag createTag(String text) throws SQLException {
        String query = "insert into tag(name) values (?)";
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties);
             PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            connection.setAutoCommit(true);
            statement.setString(1, text.toLowerCase());
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new Tag(generatedKeys.getLong(1), text.toLowerCase());
                } else {
                    throw new RuntimeException("Unable to get generated key");
                }
            }
        }
    }

    @Override
    public Allocation createAllocation(Connection connection, Transaction transaction, BudgetItem budgetItem,
                                       BigDecimal amount, String note) throws SQLException {
        String query = "insert into allocation(transactionId, budgetItemId, amount, note) values(?,?,?,?)";
        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, transaction.id());
            statement.setLong(2, budgetItem.id());
            statement.setBigDecimal(3, amount);
            statement.setString(4, note);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return new Allocation(generatedKeys.getLong(1), transaction, budgetItem, amount, note);
                } else {
                    throw new RuntimeException("Unable to get generated key");
                }
            }
        }
    }

    @Override
    public Set<Long> searchTags(String withWhat, String forWhat) throws SQLException {
        String query = String.format("select id from tag_idx where %s match ?", withWhat);
        return searchByCriteria(forWhat, query);
    }

    @Override
    public void doInTransaction(DatabaseOperation... consumers) throws SQLException {
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties)) {
            connection.setAutoCommit(false);
            for (DatabaseOperation<Connection> consumer : consumers) {
                consumer.apply(connection);
            }
            connection.setAutoCommit(true);
        }
    }

    @Override
    public void deleteAllocation(Allocation allocation) throws SQLException {
        try (Connection connection = DriverManager.getConnection(settings.getDatabaseUrl(), connectionProperties)) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement("delete from allocation where id = ?")) {
                statement.setLong(1, allocation.id());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "update transactions set allocated = false where id = ?")) {
                statement.setLong(1, allocation.transaction().id());
                statement.executeUpdate();
            }
            connection.setAutoCommit(true);
        }
    }

}
