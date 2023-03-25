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

import com.google.inject.Inject;
import hoogenbj.countary.model.Category;
import hoogenbj.countary.model.CategoryLiteHolder;
import hoogenbj.countary.model.DataModel;
import hoogenbj.countary.model.Kind;
import hoogenbj.countary.util.ParseUtils;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.StageStyle;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.Preferences;

import static hoogenbj.countary.model.Category.DEFAULT_BACKGROUND_COLOR;

public class CategoryLiteController implements ControllerHelpers {
    @FXML
    private RadioButton annualKind;
    @FXML
    private RadioButton monthlyKind;
    @FXML
    private RadioButton adhocKind;
    @FXML
    private ToggleGroup kindToggleGroup;
    @FXML
    private Button rename;
    @FXML
    private ColorPicker bgColorPicker;
    @FXML
    private Button addSibling;
    @FXML
    private Button addChild;
    @FXML
    private Button delete;
    @FXML
    private TreeView<CategoryLiteHolder> treeView;
    @Inject
    private Settings settings;
    @Inject
    private UserInterface userInterface;
    @Inject
    private DataModel model;
    private TreeItem<CategoryLiteHolder> root;

    public CategoryLiteController() {
    }

    public void initialize() {
        treeView.setCellFactory(this::makeTreeCell);
        treeView.setEditable(true);
        treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                enableButtons();
                bgColorPicker.setValue(newValue.getValue().getCategory().getBgColor());
            } else {
                disableButtons();
            }
        });
        manageCustomColors(settings, bgColorPicker);
        monthlyKind.setUserData(Kind.Monthly);
        annualKind.setUserData(Kind.Annual);
        adhocKind.setUserData(Kind.AdHoc);
        kindToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                initializeTree();
            }
        });
        initializeTree();
    }

    private void initializeTree() {
        Kind kind = (Kind) kindToggleGroup.getSelectedToggle().getUserData();
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
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve category roots", e);
        }
    }

    private TreeItem<CategoryLiteHolder> makeChild(Category item) {
        TreeItem<CategoryLiteHolder> treeItem = new TreeItem<>(new CategoryLiteHolder(item));
        treeItem.setExpanded(true);
        try {
            List<Category> children = model.getCategoryChildren(item);
            children.sort(Comparator.comparing(Category::name));
            children.forEach(child -> treeItem.getChildren().add(makeChild(child)));
            return treeItem;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve category children", e);
        }
    }

    private void disableButtons() {
        rename.setDisable(true);
        delete.setDisable(true);
        addChild.setDisable(true);
        addSibling.setDisable(true);
        bgColorPicker.setDisable(true);
    }

    private void enableButtons() {
        rename.setDisable(false);
        try {
            delete.setDisable(!model.canDeleteCategory(getCurrentTreeItem().getValue().getCategory()));
        } catch (SQLException e) {
            throw new RuntimeException("Cannot check if category can be deleted", e);
        }
        addChild.setDisable(false);
        addSibling.setDisable(false);
        bgColorPicker.setDisable(getName(treeView).equals("<Empty>"));
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

    public void onRename(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog(getName(treeView));
        dialog.setGraphic(null);
        dialog.setHeaderText(null);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Rename category");
        dialog.setContentText("Type a new name for the category:");
        dialog.showAndWait().ifPresent((name) -> {
            TreeItem<CategoryLiteHolder> item = getCurrentTreeItem();
            if (getName(treeView).equals("<Empty>")) {
                try {
                    Category parent = item.getParent() == null ? null : item.getParent().getValue().getCategory();
                    Category newCategory = model.createCategory(name,
                            (Kind) kindToggleGroup.getSelectedToggle().getUserData(), DEFAULT_BACKGROUND_COLOR, parent);
                    item.getValue().setCategory(newCategory);
                } catch (SQLException e) {
                    throw new RuntimeException("Unable to rename category", e);
                }
            } else {
                try {
                    Category newCategory = model.renameCategory(item.getValue().getCategory(), name);
                    item.getValue().setCategory(newCategory);
                } catch (SQLException e) {
                    throw new RuntimeException("Unable to rename category", e);
                }
            }
            Platform.runLater(() -> treeView.refresh());
        });
    }

    private String getName(TreeView<CategoryLiteHolder> treeView) {
        return treeView.getSelectionModel().getSelectedItem().getValue().getCategory().name();
    }

    public void onBgColor(ActionEvent actionEvent) {
        Color color = bgColorPicker.getValue();
        CategoryLiteHolder holder = getCurrentTreeItem().getValue();
        try {
            Category category = model.updateCategoryBgColor(holder.getCategory(), ParseUtils.toRGBCode(color));
            holder.setCategory(category);
            Platform.runLater(() -> treeView.refresh());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void onAddSibling(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.getEditor().setPromptText("Enter name of sibling");
        dialog.setGraphic(null);
        dialog.setHeaderText(null);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("New sibling category");
        dialog.setContentText("Type a new name for the category:");
        dialog.showAndWait().ifPresent((name) -> {
            TreeItem<CategoryLiteHolder> item = getCurrentTreeItem();
            try {
                Category newCategory = model.addCategorySibling(getCurrentTreeItem().getValue().getCategory(),
                        name);
                item.getParent().getChildren().add(new TreeItem<>(new CategoryLiteHolder(newCategory)));
                enableButtons();
            } catch (SQLException e) {
                throw new RuntimeException("Unable to rename category", e);
            }
            Platform.runLater(() -> treeView.refresh());
        });
    }

    public void onAddChild(ActionEvent actionEvent) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.getEditor().setPromptText("Enter name of child");
        dialog.setGraphic(null);
        dialog.setHeaderText(null);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("New child category");
        dialog.setContentText("Type a new name for the category:");
        dialog.showAndWait().ifPresent((name) -> {
            TreeItem<CategoryLiteHolder> item = getCurrentTreeItem();
            try {
                Category newCategory = model.addCategoryChild(getCurrentTreeItem().getValue().getCategory(),
                        name);
                item.getChildren().add(new TreeItem<>(new CategoryLiteHolder(newCategory)));
                if (!item.isExpanded())
                    item.setExpanded(true);
                enableButtons();
            } catch (SQLException e) {
                throw new RuntimeException("Unable to rename category", e);
            }
            Platform.runLater(() -> treeView.refresh());
        });
    }

    public void onDelete(ActionEvent actionEvent) {
        try {
            TreeItem<CategoryLiteHolder> item = getCurrentTreeItem();
            model.deleteCategory(item.getValue().getCategory());
            item.getParent().getChildren().remove(item);
        } catch (SQLException e) {
            throw new RuntimeException("Unexpected error when trying to delete category", e);
        }
    }

    private TreeItem<CategoryLiteHolder> getCurrentTreeItem() {
        return treeView.getSelectionModel().getSelectedItem();
    }
}
