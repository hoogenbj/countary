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

import hoogenbj.countary.model.Category;
import hoogenbj.countary.model.CategoryLiteHolder;
import hoogenbj.countary.model.DataModel;
import hoogenbj.countary.model.Kind;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class CategoryLiteTreeView extends StackPane {

    @FXML
    private TreeView<CategoryLiteHolder> treeView;
    private Kind kind;
    private DataModel model;
    private TreeItem<CategoryLiteHolder> root;
    private BiConsumer<Category, Boolean> selectedCategoryCallback;
    private Category selectedCategory;
    private TreeItem<CategoryLiteHolder> found;

    public static CategoryLiteTreeView getInstance(DataModel model, Kind kind, Category selectedCategory, BiConsumer<Category, Boolean> selectedCategoryCallback) {
        FXMLLoader fxmlLoader = new FXMLLoader(CategoryLiteTreeView.class.getResource("CategoryLiteTreeView.fxml"));
        CategoryLiteTreeView root = new CategoryLiteTreeView();
        root.kind = kind;
        root.model = model;
        root.selectedCategory = selectedCategory;
        root.selectedCategoryCallback = selectedCategoryCallback;
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(root);
        try {
            fxmlLoader.load();
            return root;
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    @FXML
    private void initialize() {
        treeView.setCellFactory(this::makeTreeCell);
        treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                selectedCategoryCallback.accept(newValue.getValue().getCategory(), newValue.isLeaf());
            }
        });
        initializeTree();
        if (found != null)
            treeView.getSelectionModel().select(found);
    }

    private TreeCell<CategoryLiteHolder> makeTreeCell(TreeView<CategoryLiteHolder> categoryHolderTreeView) {
        return new TreeCell<>() {
            @Override
            protected void updateItem(CategoryLiteHolder item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setGraphic(CategoryControlLite.getInstance(item.getCategory()));
                }
            }
        };
    }

    private void initializeTree() {
        try {
            List<Category> roots = model.getCategoryRoots(kind);
            roots.sort(Comparator.comparing(Category::name));
            root = new TreeItem<>(new CategoryLiteHolder());
            if (roots.isEmpty()) {
                root.getChildren().add(new TreeItem<>(
                        new CategoryLiteHolder(new Category(null, "<Empty>", "", null, null))));
            } else {
                roots.forEach(item -> root.getChildren().add(makeChild(item)));
            }
            treeView.setRoot(root);
            root.setExpanded(true);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve category roots", e);
        }
    }

    private TreeItem<CategoryLiteHolder> makeChild(Category item) {
        TreeItem<CategoryLiteHolder> treeItem = new TreeItem<>(new CategoryLiteHolder(item));
        try {
            List<Category> children = model.getCategoryChildren(item);
            children.sort(Comparator.comparing(Category::name));
            children.forEach(child -> treeItem.getChildren().add(makeChild(child)));
            treeItem.setExpanded(true);
            if (!Objects.isNull(selectedCategory) && Objects.equals(item.id(), selectedCategory.id())) {
                found = treeItem;
            }
            return treeItem;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve category children", e);
        }
    }
}
