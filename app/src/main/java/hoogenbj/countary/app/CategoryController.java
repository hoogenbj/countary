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

import hoogenbj.countary.model.*;
import hoogenbj.countary.util.ParseUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.text.Text;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class CategoryController {
    private final Kind kind;
    @FXML
    private TreeTableView<CategoryHolder> treeTableView;
    @FXML
    private TreeTableColumn<CategoryHolder, CategoryHolder> categoryColumn;
    @FXML
    private TreeTableColumn<CategoryHolder, String> plannedColumn;
    @FXML
    private TreeTableColumn<CategoryHolder, String> actualColumn;
    @FXML
    private Text balance;
    private final DataModel model;
    private TreeItem<CategoryHolder> root;

    private final CategoryModel categoryModel;

    public CategoryController(DataModel model, Kind kind, CategoryModel categoryModel) {
        this.model = model;
        this.kind = kind;
        this.categoryModel = categoryModel;
    }

    public Node createNode() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("Category.fxml"));
        loader.setController(this);
        return loader.load();
    }

    public void initialize() {
        actualColumn.setCellValueFactory(v -> v.getValue().getValue().actualProperty());
        plannedColumn.setCellValueFactory(v -> v.getValue().getValue().plannedProperty());
        categoryColumn.setCellValueFactory(v -> v.getValue().valueProperty());
        categoryColumn.setCellFactory(this::makeCategoryCell);
        initializeTree();
        categoryModel.getBudgetHolder().balanceProperty().addListener((observable, oldValue, newValue) -> {
            if (Objects.nonNull(newValue) && !newValue.equals(oldValue)) {
                balance.setText(String.format("Actual Balance: %s", ParseUtils.formatBigDecimal(newValue)));
            }
        });
        BigDecimal balanceValue = categoryModel.getBalance();
        if (balanceValue != null)
            balance.setText(String.format("Actual Balance: %s", ParseUtils.formatBigDecimal(balanceValue)));
        else
            balance.setText("");
    }

    private TreeTableCell<CategoryHolder, CategoryHolder> makeCategoryCell(TreeTableColumn<CategoryHolder, CategoryHolder> categoryHolderStringTreeTableColumn) {
        return new CategoryTreeTableCell() {

            @Override
            public CategoryControl getCategoryControl() {
                return CategoryControl.getInstance();
            }
        };
    }

    private void initializeTree() {
        try {
            List<Category> roots = new java.util.ArrayList<>(model.getCategoryRoots(kind).stream()
                    .filter(category -> categoryModel.getCategories().contains(category)
                    ).toList());
            roots.sort(Comparator.comparing(Category::name));
            root = new TreeItem<>(new CategoryHolder(new Category(-1L, "root", "", kind, null)));
            if (roots.isEmpty()) {
                root.getChildren().add(new CheckBoxTreeItem<>(
                        new CategoryHolder(new Category(null, "<Empty>", "", null, null))));
            } else {
                roots.forEach(category -> root.getChildren().add(makeChild(category)));
            }
            treeTableView.setRoot(root);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve category roots", e);
        }
    }

    private TreeItem<CategoryHolder> makeChild(Category cat) {
        CategoryHolder holder = categoryModel.getCategoryLookup().get(cat);
        TreeItem<CategoryHolder> treeItem = new TreeItem<>(holder);
        treeItem.setExpanded(true);
        try {
            List<Category> children = new java.util.ArrayList<>(model.getCategoryChildren(cat).stream()
                    .filter(category -> categoryModel.getCategories().contains(category)).toList());
            children.sort(Comparator.comparing(Category::name));
            children.forEach(category -> treeItem.getChildren().add(makeChild(category)));
            return treeItem;
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve category children", e);
        }
    }

}
