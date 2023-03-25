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

package hoogenbj.countary.app;

import java.sql.*;
import java.util.*;

public class TestQueries {

    static Set<Set<Long>> collapse(Collection<Set<Long>> collection) {
        Tracker tracker = new Tracker();
        collection.forEach(set -> {
            Set<Long> intersection = new HashSet<>(tracker.ptr);
            intersection.retainAll(set);
            if (intersection.isEmpty()) {
                if (!tracker.ptr.isEmpty())
                    tracker.seen.add(tracker.ptr);
                tracker.seen.add(set);
                tracker.ptr = set;
            } else {
                Set<Long> copy = new HashSet<>(tracker.ptr);
                copy.addAll(set);
                tracker.seen.remove(set);
                tracker.seen.remove(tracker.ptr);
                tracker.seen.add(copy);
                tracker.ptr = copy;
            }

        });
        return tracker.seen;
    }

    public static void main(String... args) throws SQLException {
        String filePath = "/Users/johan/Developer/data/bajeti.db";
        String dbUrl = String.format("jdbc:sqlite:%s", filePath);
        Map<Long, Set<Long>> lookup = new HashMap<>();
        String query = "select itemId,tagId from item_tag where itemId in ( select itemId from budget_item where budgetId=2)";
        try (Connection connection = DriverManager.getConnection(dbUrl);
             PreparedStatement statement = connection.prepareStatement(query)) {
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Long itemId = rs.getLong(1);
                    Long tagId = rs.getLong(2);
                    Set<Long> tags = lookup.computeIfAbsent(itemId, k -> new HashSet<>());
                    tags.add(tagId);
                }
            }
        }
        System.out.printf("lookup values size: %d%n", lookup.values().size());
        lookup.values().forEach(System.out::println);
        Set<Set<Long>> collapsed = collapse(lookup.values());
        int count;
        // Keep calling collapse until there is nothing to collapse
        do {
            count = collapsed.size();
            collapsed = collapse(collapsed);
        } while (collapsed.size() < count);
        System.out.printf("collapsed size: %d%n", collapsed.size());
        collapsed.forEach(System.out::println);
    }

    static class Tracker {
        public Set<Long> ptr = Collections.emptySet();
        public Set<Set<Long>> seen = new HashSet<>();
    }
}
