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
import java.util.Objects;
import java.util.Set;

public record BudgetItem(Long id, Budget budget, Item item, String note, BigDecimal planned, Set<Tag> tags,
                         Boolean canDelete) {
    public BudgetItem(Long id, Budget budget, Item item, String note, String planned, Set<Tag> tags, Boolean canDelete) {
        this(id, budget, item, note, new BigDecimal(planned), tags, canDelete);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BudgetItem that = (BudgetItem) o;
        return Objects.equals(id, that.id) && Objects.equals(budget, that.budget) && Objects.equals(item, that.item);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, budget, item);
    }
}
