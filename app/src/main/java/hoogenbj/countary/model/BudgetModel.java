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

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.*;

public class BudgetModel {
    private final DataModel dataModel;

    public BudgetModel(DataModel dataModel) {
        this.dataModel = dataModel;
    }

    private static class Tracker {
        public Set<Tag> ptr = Collections.emptySet();
        public Set<Set<Tag>> seen = new HashSet<>();
    }

    private Set<Set<Tag>> collapse(Collection<Set<Tag>> collection) {
        Tracker tracker = new Tracker();
        collection.forEach(set -> {
            Set<Tag> intersection = new HashSet<>(tracker.ptr);
            intersection.retainAll(set);
            if (intersection.isEmpty()) {
                if (!tracker.ptr.isEmpty())
                    tracker.seen.add(tracker.ptr);
                tracker.seen.add(set);
                tracker.ptr = set;
            } else {
                Set<Tag> copy = new HashSet<>(tracker.ptr);
                copy.addAll(set);
                tracker.seen.remove(set);
                tracker.seen.remove(tracker.ptr);
                tracker.seen.add(copy);
                tracker.ptr = copy;
            }

        });
        return tracker.seen;
    }

    public List<List<BudgetTagProfile>> getBudgetTagProfile(Budget budget) throws SQLException {
        Collection<Set<Tag>> ftags = dataModel.getItemTagsForBudget(budget);
        Set<Set<Tag>> collapsed = collapse(ftags);
        int count;
        // Keep calling collapse until there is nothing to collapse
        do {
            count = collapsed.size();
            collapsed = collapse(collapsed);
        } while (collapsed.size() < count);
        Map<Long, Integer> quantifiedTags = dataModel.getQuantifiedTagOrder();
        List<List<BudgetTagProfile>> budgetTagProfile = collapsed.stream().map(set -> set.stream()
                .map(t -> new BudgetTagProfile(t.id(), t.name()))
                .sorted((left, right) -> quantifiedTags.get(right.getTagId()).compareTo(quantifiedTags.get(left.getTagId())))
                .toList()
        ).toList();
        Map<Long, BigDecimal> planned = dataModel.getPlannedByBudgetAndTags(budget);
        budgetTagProfile.forEach(bdp -> {
            bdp.forEach(p -> p.setTotalPlanned(planned.computeIfAbsent(p.getTagId(), k -> BigDecimal.ZERO)));
        });
        Map<Long, BigDecimal> actual = dataModel.getActualByBudgetAndTags(budget);
        budgetTagProfile.forEach(bdp -> {
            bdp.forEach(p -> p.setTotalActual(actual.computeIfAbsent(p.getTagId(), k -> BigDecimal.ZERO)));
        });
        return budgetTagProfile;
    }
}
