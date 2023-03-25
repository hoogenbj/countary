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

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;
import java.util.Set;

public class TagHolder {
    private StringProperty name;
    private Long id;
    private Set<Item> items;

    public static TagHolder of(Set<Item> items, Tag tag) {
        if (!items.isEmpty())
            return new TagHolder(items, tag);
        else
            return null;
    }

    private TagHolder(Set<Item> items, Tag tag) {
        this(tag.name(), tag.id());
        this.items = items;
    }

    public TagHolder(String name, Long id) {
        setName(name);
        this.id = id;
    }

    public Set<Item> getItems() {
        return items;
    }

    public void setItems(Set<Item> items) {
        this.items = items;
    }

    public String name() {
        return nameProperty().get();
    }

    public StringProperty nameProperty() {
        if (name == null) name = new SimpleStringProperty(this, "name");
        return name;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return name.get();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TagHolder tagHolder = (TagHolder) o;
        return id.equals(tagHolder.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String getName() {
        return nameProperty().get();
    }

    public void setName(String name) {
        this.nameProperty().set(name);
    }
}
