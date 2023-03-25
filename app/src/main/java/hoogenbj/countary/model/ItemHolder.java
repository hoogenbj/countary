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

import hoogenbj.countary.app.KindProperty;
import javafx.beans.property.*;

import java.util.Objects;
import java.util.function.BiFunction;

public class ItemHolder {
    private String desc;
    private Kind cat;
    private LongProperty id;
    private Long rawid;
    private SimpleObjectProperty<Item> itemObservableValue;
    private SimpleObjectProperty<Category> categoryProperty;
    private StringProperty name;
    private KindProperty kind;

    public ItemHolder() {
    }

    public ItemHolder(Item item, BiFunction<Item, String, Item> onNameChanged) {
        this(item.id(), item.name(), item.kind(), item.category());
        setItem(item);
        this.nameProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                itemProperty().set(onNameChanged.apply(itemProperty().get(), newValue));
            }
        });
    }

    public ItemHolder(Item item) {
        this(item.id(), item.name(), item.kind(), item.category());
        setItem(item);
    }

    public ItemHolder(Long id, String name, Kind kind, Category category) {
        this.desc = name;
        this.cat = kind;
        this.rawid = id;
        setName(name);
        setCategory(category);
        setKind(kind);
        setId(id);
    }

    public void setItem(Item item) {
        this.itemProperty().set(item);
    }

    public Item getItem() {
        return itemProperty().get();
    }

    public Kind cat() {
        return cat;
    }

    public String desc() {
        return desc;
    }

    public SimpleObjectProperty<Category> categoryProperty() {
        if (categoryProperty == null) {
            categoryProperty = new SimpleObjectProperty<>(this, "category");
        }
        return categoryProperty;
    }

    public void setCategory(Category category) {
        this.categoryProperty().set(category);
    }

    public StringProperty nameProperty() {
        if (name == null) name = new SimpleStringProperty(this, "name");
        return name;
    }

    public KindProperty kindProperty() {
        if (kind == null) kind = new KindProperty(this, "kind");
        return kind;
    }

    public LongProperty idProperty() {
        if (id == null) id = new SimpleLongProperty(this, "id");
        return id;
    }

    public Long getId() {
        return idProperty().get();
    }

    public void setId(Long id) {
        this.idProperty().set(id);
    }

    public Kind getKind() {
        return kindProperty().get();
    }

    public void setKind(Kind kind) {
        this.kindProperty().set(kind);
    }


    public String getName() {
        return nameProperty().get();
    }

    public void setName(String name) {
        this.nameProperty().set(name);
    }

    public SimpleObjectProperty<Item> itemProperty() {
        if (itemObservableValue == null) {
            itemObservableValue = new SimpleObjectProperty<>(this, "item");
        }
        return itemObservableValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemHolder that = (ItemHolder) o;
        return rawid.equals(that.rawid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawid);
    }
}
