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
import hoogenbj.countary.util.SVGUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static hoogenbj.countary.util.ParseUtils.DECIMAL_FORMAT_SYMBOLS;

public class BudgetItemWorksheetController implements ControllerHelpers {

    private final BudgetHolder budgetHolder;
    private final UserInterface userInterface;
    private final boolean noAddItem;
    private final boolean noDeleteColumn;

    @FXML
    private ClearableTextField searchCriteria;

    @FXML
    private HBox topPane;

    @FXML
    private TableColumn<BudgetItemHolder, String> itemNoteColumn;

    @FXML
    private TableColumn<BudgetItemHolder, BigDecimal> itemPlannedColumn;
    @FXML
    private TableColumn<BudgetItemHolder, BigDecimal> itemActualColumn;

    @FXML
    private TableColumn<BudgetItemHolder, Category> itemCategoryColumn;
    @FXML
    private TableColumn<BudgetItemHolder, BudgetItem> deleteColumn;
    @FXML
    private TableColumn<BudgetItemHolder, String> itemNameColumn;

    @FXML
    private TableColumn<BudgetItemHolder, BudgetItem> itemTagsColumn;

    @FXML
    private TableView<BudgetItemHolder> tableView;

    @FXML
    private Button calculate;

    private final DataModel model;

    private ObservableList<BudgetItemHolder> listOfBudgetItems;
    private Map<BudgetItem, BudgetItemHolder> holderLookup = new HashMap<>();
    private FilteredList<BudgetItemHolder> filteredBudgetItemList;
    private boolean searchingText;

    public FilteredList<BudgetItemHolder> getFilteredBudgetItemList() {
        return filteredBudgetItemList;
    }

    private final Map<Long, BudgetItemHolder> holderMap = new HashMap<>();

    private boolean initDone = false;

    private final Consumer<BudgetItemHolder> itemSelectionNotifier;
    @FXML
    private HBox addItemContainer;
    private Map<String, BudgetTagProfile> profileMap;
    private CategoryModel categoryModel;
    private Set<Long> budgetItems = Collections.emptySet();

    public void setProfileMap(Map<String, BudgetTagProfile> profileMap) {
        this.profileMap = profileMap;
    }

    public CategoryModel getCategoryModel() {
        return categoryModel;
    }

    public BudgetItemWorksheetController(UserInterface userInterface, DataModel model, BudgetHolder budgetHolder,
                                         boolean noAddItem, boolean noDeleteColumn, Consumer<BudgetItemHolder> itemSelectionNotifier) {
        this.model = model;
        this.budgetHolder = budgetHolder;
        this.userInterface = userInterface;
        this.noAddItem = noAddItem;
        this.itemSelectionNotifier = itemSelectionNotifier;
        this.noDeleteColumn = noDeleteColumn;
    }

    public BudgetItemWorksheetController(UserInterface userInterface, DataModel model, BudgetHolder budgetHolder,
                                         boolean noAddItem, boolean noDeleteColumn) {
        this(userInterface, model, budgetHolder, noAddItem, noDeleteColumn, null);
    }

    public Node createNode() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("BudgetItemWorksheet.fxml"));
        loader.setController(this);
        return loader.load();
    }

    @FXML
    private void initialize() {
        initControls();
        loadBudgetItems();
        initDone = true;
    }

    private void loadBudgetItems() {
        try {
            listOfBudgetItems =
                    FXCollections.observableArrayList(model.getBudgetItemHolders(budgetHolder.getBudget(), this::onPlannedChange, this::onNoteChange));
            Map<Category, Set<BudgetItemHolder>> categoryItems = new HashMap<>();
            Set<Category> categories = model.getBudgetCategories(budgetHolder.getBudget());
            listOfBudgetItems.forEach(item -> {
                Set<BudgetItemHolder> set = categoryItems.computeIfAbsent(item.categoryProperty().get(), k -> new HashSet<>());
                set.add(item);
                holderLookup.put(item.getBudgetItem(), item);
            });
            categoryModel = new CategoryModel(budgetHolder, categories, categoryItems);
            filteredBudgetItemList = new FilteredList<>(listOfBudgetItems);
            SortedList<BudgetItemHolder> sortedList = new SortedList<>(filteredBudgetItemList, Comparator.comparing(BudgetItemHolder::getName));
            tableView.setItems(sortedList);
            listOfBudgetItems.forEach(holder -> holderMap.put(holder.getId(), holder));
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load budget items", e);
        }
    }

    private void initControls() {
        if (noAddItem) {
            addItemContainer.setVisible(false);
            addItemContainer.setManaged(false);
        }
        if (noDeleteColumn) {
            deleteColumn.setVisible(false);
        } else {
            deleteColumn.setCellFactory((column) -> this.makeDeleteButton(column, this::makeButtonGraphic));
            deleteColumn.setCellValueFactory(p -> p.getValue().budgetItemProperty());
            deleteColumn.getStyleClass().add("delete-column");
        }
        itemNoteColumn.setCellValueFactory(f -> f.getValue().noteProperty());
        itemNoteColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        itemPlannedColumn.setCellValueFactory(f -> f.getValue().plannedProperty());
        itemPlannedColumn.setCellFactory(this::makePlannedCell);
        itemActualColumn.setCellValueFactory(f -> f.getValue().actualProperty());
        itemActualColumn.setCellFactory(this::makeBigDecimalCell);
        itemNameColumn.setCellValueFactory(f -> f.getValue().nameProperty());
        itemCategoryColumn.setCellValueFactory(p -> p.getValue().categoryProperty());
        itemCategoryColumn.setCellFactory(this::makeCategoryCell);
        itemTagsColumn.setCellValueFactory(f -> f.getValue().budgetItemProperty());
        itemTagsColumn.setCellFactory(this::makeTagsCell);
        // Make the tags and note columns divide available space between them
        if (noDeleteColumn) {
            itemTagsColumn.prefWidthProperty()
                    .bind(tableView.widthProperty()
                            .subtract(itemPlannedColumn.widthProperty())
                            .subtract(itemActualColumn.widthProperty())
                            .subtract(itemCategoryColumn.widthProperty())
                            .subtract(itemNameColumn.widthProperty()).divide(2.0).subtract(2.0 * 5.0));
            itemNoteColumn.prefWidthProperty()
                    .bind(tableView.widthProperty()
                            .subtract(itemPlannedColumn.widthProperty())
                            .subtract(itemActualColumn.widthProperty())
                            .subtract(itemCategoryColumn.widthProperty())
                            .subtract(itemNameColumn.widthProperty()).divide(2.0).subtract(2.0 * 5.0));
        } else {
            itemTagsColumn.prefWidthProperty()
                    .bind(tableView.widthProperty()
                            .subtract(deleteColumn.widthProperty())
                            .subtract(itemPlannedColumn.widthProperty())
                            .subtract(itemActualColumn.widthProperty())
                            .subtract(itemCategoryColumn.widthProperty())
                            .subtract(itemNameColumn.widthProperty()).divide(2.0).subtract(2.0 * 5.0));
            itemNoteColumn.prefWidthProperty()
                    .bind(tableView.widthProperty()
                            .subtract(deleteColumn.widthProperty())
                            .subtract(itemPlannedColumn.widthProperty())
                            .subtract(itemActualColumn.widthProperty())
                            .subtract(itemCategoryColumn.widthProperty())
                            .subtract(itemNameColumn.widthProperty()).divide(2.0).subtract(2.0 * 5.0));
        }
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.getSelectionModel().getSelectedItems()
                .addListener((ListChangeListener<? super BudgetItemHolder>) c -> monitorSelectionChanges(c.getList()));
        if (this.itemSelectionNotifier != null)
            tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue != null && !newValue.equals(oldValue)) {
                    this.itemSelectionNotifier.accept(newValue);
                } else {
                    this.itemSelectionNotifier.accept(null);
                }
            });
        searchCriteria.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.length() > 1 && !newValue.equals(oldValue)) {
                searchingText = true;
                doSearch(newValue);
                updatePredicate();
            } else {
                searchingText = false;
                budgetItems = Collections.emptySet();
                updatePredicate();
            }
        });
        calculate.setOnAction(this::calculate);
    }

    private void monitorSelectionChanges(ObservableList<? extends BudgetItemHolder> list) {
        calculate.setDisable(list.size() == 0);
    }

    private Future<Button> makeButtonGraphic(BudgetItemHolder holder) {
        return CompletableFuture.supplyAsync(() -> {
            Button btn = new Button();
            btn.setGraphic(SVGUtils.makeDeleteIcon());
            btn.disableProperty().bind(holder.canDeleteProperty().not());
            btn.setOnAction(event -> {
                try {
                    if (model.canDeleteBudgetItem(holder.getBudgetItem())) {
                        model.deleteBudgetItem(holder.getBudgetItem());
                        Platform.runLater(() -> {
                            listOfBudgetItems.remove(holderLookup.get(holder.getBudgetItem()));
                            tableView.refresh();
                        });
                    } else {
                        userInterface.showError("Cannot delete budget item. Remove it from all allocations first");
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Cannot check whether it is safe to delete budget item or delete it");
                }
            });
            return btn;
        });
    }

    private void doSearch(String criteria) {
        try {
            if (!criteria.contains("*"))
                criteria = criteria + "*";
            budgetItems = model.searchBudgetItems(budgetHolder.getBudget(), criteria);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to search budgetItems", e);
        }
    }

    private Predicate<? super BudgetItemHolder> getPredicate() {
        return this::filterText;
    }

    private boolean filterText(BudgetItemHolder holder) {
        if (!searchingText)
            return true;
        else
            return budgetItems.contains(holder.getId());
    }

    public BudgetItemHolder getSelected() {
        return tableView.getSelectionModel().getSelectedItem();
    }

    public void onBudgetItemChanged(BudgetItem budgetItem) {
        budgetItem.tags().forEach(tag -> {
            try {
                BigDecimal actual = model.getActualByBudgetAndTag(budgetHolder.getBudget(), tag);
                if (profileMap.containsKey(tag.name()))
                    profileMap.get(tag.name()).setTotalActual(actual);
            } catch (SQLException e) {
                throw new RuntimeException(String.format("Unable to retrieve actual for budget %s and tag %s",
                        budgetHolder.getBudget().name(), tag.name()), e);
            }
        });
        BigDecimal actual;
        try {
            actual = model.getActualForBudgetItem(budgetItem);
            BudgetItemHolder holder = holderMap
                    .computeIfAbsent(budgetItem.id(),
                            key -> {
                                BudgetItemHolder h = new BudgetItemHolder(budgetItem, this::onPlannedChange, this::onNoteChange);
                                holderLookup.put(budgetItem, h);
                                return h;
                            });
            holder.setActual(actual);
            if (categoryModel.getBudgetHolder().getBudget().equals(holder.getBudgetItem().budget())) {
                categoryModel.updateActual(holder);
            }
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Unable to retrieve actual for budget item %s",
                    budgetItem.item().name()), e);
        }
        holderLookup.get(budgetItem).setBudgetItem(budgetItem);
        updatePredicate();
    }

    private void updatePredicate() {
        filteredBudgetItemList.setPredicate(getPredicate());
    }

    private TableCell<BudgetItemHolder, Category> makeCategoryCell(TableColumn<BudgetItemHolder, Category> tableColumn) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);
                this.setText(null);
                this.setGraphic(null);
                if (!empty && category != null) {
                    CategoryControlLite control = CategoryControlLite.getInstance(category);
                    this.setGraphic(control);
                }
            }
        };
    }

    private TableCell<BudgetItemHolder, BigDecimal> makePlannedCell(TableColumn<BudgetItemHolder, BigDecimal> tableColumn) {
        TextFieldTableCell<BudgetItemHolder, BigDecimal> cell = new TextFieldTableCell<>();
        cell.setConverter(new StringConverter<>() {
            final DecimalFormat format = new DecimalFormat(DECIMAL_FORMAT_SYMBOLS, DecimalFormatSymbols.getInstance(Locale.ENGLISH));

            public String toString(BigDecimal object) {
                return format.format(object);
            }

            public BigDecimal fromString(String string) {
                try {
                    Number number = format.parse(string);
                    if (number instanceof Long)
                        return new BigDecimal((Long) number);
                    else if (number instanceof Double)
                        return BigDecimal.valueOf((Double) number);
                    else
                        throw new IllegalStateException("Unexpected value: " + number);
                } catch (Throwable e) {
                    cell.cancelEdit();
                    throw new RuntimeException("Unexpected error editing planned value", e);
                }
            }
        });
        return cell;
    }

    private TableCell<BudgetItemHolder, BudgetItem> makeTagsCell(TableColumn<BudgetItemHolder, BudgetItem> col) {
        return new TableCell<>() {
            @Override
            protected void updateItem(BudgetItem item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);
                if (!empty && item != null) {
                    this.setGraphic(DisplayTagsControl.getInstance(item));
                }
            }
        };
    }

    @FXML
    private void onAddItemClicked(ActionEvent actionEvent) {
        SelectItemDlg dlg = SelectItemDlg.getInstance(CountaryApp.OWNER_WINDOW, model,
                budgetHolder.getBudget());
        dlg.showAndWait().ifPresent(this::addItem);
    }

    private void addItem(List<Item> items) {
        items.forEach(item -> {
            CreateBudgetItemDlgController dlg = CreateBudgetItemDlgController.getInstance(userInterface, model, budgetHolder.getBudget(), item);
            dlg.showAndWait().ifPresent(budgetItem -> {
                BudgetItemHolder holder = new BudgetItemHolder(budgetItem, this::onPlannedChange, this::onNoteChange);
                listOfBudgetItems.add(holder);
                holderLookup.put(budgetItem, holder);
            });
        });
    }

    private BudgetItem onPlannedChange(BudgetItem budgetItem, BigDecimal bigDecimal) {
        if (initDone) {
            try {
                BudgetItem newBudgetItem = model.updateBudgetItemPlanned(budgetItem, bigDecimal);
                budgetItem.tags().forEach(tag -> {
                    try {
                        BigDecimal planned = model.getPlannedByBudgetAndTag(budgetItem.budget(), tag);
                        if (profileMap != null)
                            profileMap.get(tag.name()).setTotalPlanned(planned);
                    } catch (SQLException e) {
                        throw new RuntimeException(String.format("Unable to retrieve planned for budget %s and tag %s",
                                budgetItem.budget().name(), tag.name()), e);
                    }
                });
                BudgetItemHolder holder = holderMap.get(budgetItem.id());
                categoryModel.updatePlanned(holder);
                return newBudgetItem;
            } catch (SQLException e) {
                throw new RuntimeException("Unable to update planned value", e);
            }
        }
        return budgetItem;
    }

    private BudgetItem onNoteChange(BudgetItem budgetItem, String s) {
        if (initDone) {
            try {
                return model.updateBudgetItemNote(budgetItem, s);
            } catch (SQLException e) {
                throw new RuntimeException("Unable to update note", e);
            }
        }
        return budgetItem;
    }

    private void calculate(ActionEvent btn) {
        List<BigDecimal> planned = tableView.getSelectionModel().getSelectedItems().stream().map(BudgetItemHolder::getPlanned).toList();
        List<BigDecimal> actual = tableView.getSelectionModel().getSelectedItems().stream().map(BudgetItemHolder::getActual).toList();
        CalculateDlgController dialog = CalculateDlgController.getInstance(CountaryApp.OWNER_WINDOW, planned, actual);
        dialog.showAndWait();
    }
}
