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

import hoogenbj.countary.util.ParseUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;
import java.util.Objects;

public class BudgetTagProfile {
    private final Long tagId;
    private final String tagName;
    private StringProperty totalPlanned;
    private StringProperty totalActual;

    public BudgetTagProfile(Long tagId, String tagName) {
        this.tagId = tagId;
        this.tagName = tagName;
    }

    public Long getTagId() {
        return tagId;
    }

    public String getTagName() {
        return tagName;
    }

    public String getTotalPlanned() {
        return totalPlannedProperty().get();
    }

    public StringProperty totalPlannedProperty() {
        if (totalPlanned == null) {
            totalPlanned = new SimpleStringProperty(this, "totalPlanned");
        }
        return totalPlanned;
    }

    public void setTotalPlanned(BigDecimal totalPlanned) {
        totalPlannedProperty().set(ParseUtils.formatBigDecimal(totalPlanned));
    }

    public String getTotalActual() {
        return totalActualProperty().get();
    }

    public StringProperty totalActualProperty() {
        if (totalActual == null) {
            totalActual = new SimpleStringProperty(this, "totalActual");
        }
        return totalActual;
    }

    public void setTotalActual(BigDecimal totalActual) {
        totalActualProperty().set(ParseUtils.formatBigDecimal(Objects.requireNonNullElse(totalActual, BigDecimal.ZERO)));
    }

    @Override
    public String toString() {
        return "BudgetTagProfile{" +
                "tagId=" + tagId +
                ", tagName='" + tagName + '\'' +
                ", totalPlanned=" + totalPlanned.get() +
                ", totalActual=" + totalActual.get() +
                '}';
    }
}
