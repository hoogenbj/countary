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

package hoogenbj.countary.app;

import hoogenbj.countary.model.*;
import hoogenbj.countary.util.ParseUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeType;
import javafx.scene.text.Text;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CategoryController {
    private final Kind kind;
    @FXML
    private TreeTableView<CategoryHolder> treeTableView;
    @FXML
    private HBox balanceBox;
    @FXML
    private TreeTableColumn<CategoryHolder, CategoryHolder> categoryColumn;
    @FXML
    private TreeTableColumn<CategoryHolder, String> plannedColumn;
    @FXML
    private TreeTableColumn<CategoryHolder, String> actualColumn;
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
                updateBalances(categoryModel.getBudgetHolder().getBudget());
            }
        });
        updateBalances(categoryModel.getBudgetHolder().getBudget());
    }

    private void updateBalances(Budget budget) {
        try {
            Map<Account, BigDecimal> balances = model.calculateBalances(budget);
            balanceBox.getChildren().clear();
            String heading = "Actual balance:";
            if (balances.size() > 1)
                heading = "Actual balances:";
            Text head = getBalanceHeading(heading);
            balanceBox.getChildren().add(head);
            balances.forEach((key, value) -> {
                HBox hBox = makeBalanceBox(key, value);
                balanceBox.getChildren().add(hBox);
            });
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve balances for " + budget.name(), e);
        }
    }

    private static Text getBalanceHeading(String heading) {
        Text head = new Text(heading);
        head.setStrokeType(StrokeType.OUTSIDE);
        head.setStrokeWidth(0.0);
        head.getStyleClass().add("balance-text");
        return head;
    }

    private static HBox makeBalanceBox(Account account, BigDecimal balance) {
        SVGPath tag = new SVGPath();
        tag.setContent(AccountTag.svgPathContent);
        tag.setFill(Color.web(account.tagColor()));
        Text text = new Text(ParseUtils.formatBigDecimal(balance));
        text.setStrokeType(StrokeType.OUTSIDE);
        text.setStrokeWidth(0.0);
        text.getStyleClass().add("balance-text");
        HBox hBox = new HBox(tag, text);
        hBox.setSpacing(2.0);
        hBox.setAlignment(Pos.CENTER);
        return hBox;
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
            treeTableView.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
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
