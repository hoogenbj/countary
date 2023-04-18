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
import hoogenbj.countary.model.*;
import hoogenbj.countary.util.DbUtils;
import hoogenbj.countary.util.SVGUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

import static org.sqlite.core.Codes.SQLITE_CONSTRAINT;

public class ItemsController implements ControllerHelpers {

    @FXML
    private TableColumn<ItemHolder, Item> categoryColumn;
    @FXML
    private Button tagButton;

    @FXML
    private ScrollPane bottom;

    @FXML
    private ToggleGroup kindToggleGroup;

    @FXML
    private RadioButton annualKind;

    @FXML
    private RadioButton monthlyKind;

    @FXML
    private RadioButton adhocKind;

    @FXML
    private RadioButton noKind;

    @FXML
    private TextField searchCriteria;

    @FXML
    private TableView<ItemHolder> tableView;

    @FXML
    private TableColumn<ItemHolder, String> nameColumn;

    @FXML
    private TableColumn<ItemHolder, Kind> kindColumn;

    @FXML
    private TableColumn<ItemHolder, Item> tagsColumn;

    @FXML
    private TableColumn<ItemHolder, Item> deleteColumn;

    @Inject
    private DataModel model;

    @Inject
    private UserInterface userInterface;

    @Inject
    private Settings settings;

    private ObservableList<ItemHolder> listOfItems;
    private FilteredList<ItemHolder> filteredList;
    private boolean initDone = false;
    private Map<Item, Future<TaggingControl>> taggingControlLookup = new ConcurrentHashMap<>();
    private final Map<Item, Future<Button>> deleteButtonLookup = new ConcurrentHashMap<>();
    private final Map<Item, Future<CategoryControlLite>> categoryControlLookup = new ConcurrentHashMap<>();

    // TaggingControl needs to share one otherwise other controls won't see new tags just added
    private ObservableList<TagHolder> tagHolderList;

    public void initialize() {
        try {
            tagHolderList = FXCollections.observableArrayList(model.getTags());
        } catch (SQLException e) {
            throw new RuntimeException("Unable to retrieve all tags", e);
        }
        loadData();
        initDone = true;
        startBackgroundTasks();
        initControls();
    }

    private void startBackgroundTasks() {
        listOfItems.forEach(holder -> taggingControlLookup.putIfAbsent(holder.getItem(),
                CompletableFuture.supplyAsync(() -> TaggingControl.getInstance(userInterface, model, Set.of(holder.getItem()), tagHolderList))));
        listOfItems.forEach(holder -> deleteButtonLookup.putIfAbsent(holder.getItem(), makeButtonGraphic(holder)));
        listOfItems.forEach(holder -> categoryControlLookup
                .putIfAbsent(holder.getItem(), CompletableFuture.supplyAsync(() -> makeCategoryLiteControl(holder.getItem().category()))));
    }

    private void loadData() {
        try {
            listOfItems = FXCollections.observableArrayList(model.getItems().stream()
                    .map(f -> new ItemHolder(f, this::onNameChanged)).toList());
            filteredList = new FilteredList<>(listOfItems);
            SortedList<ItemHolder> sortedList = new SortedList<>(filteredList, Comparator.comparing(ItemHolder::desc));
            tableView.setItems(sortedList);
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load items", e);
        }
    }

    private Item onNameChanged(Item item, String s) {
        if (initDone) {
            try {
                return model.updateItemName(item, s);
            } catch (SQLException e) {
                throw new RuntimeException("Unable to update item name", e);
            }
        }
        return item;
    }

    private void initControls() {
        annualKind.setUserData(Kind.Annual);
        monthlyKind.setUserData(Kind.Monthly);
        adhocKind.setUserData(Kind.AdHoc);
        noKind.setUserData(Kind.None);
        deleteColumn.setCellFactory((column) -> this.makeDeleteButton(column, this::makeButtonGraphic));
        deleteColumn.setCellValueFactory(p -> p.getValue().itemProperty());
        deleteColumn.getStyleClass().add("delete-column");
        nameColumn.setCellValueFactory(p -> p.getValue().nameProperty());
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        categoryColumn.setCellValueFactory(p -> p.getValue().itemProperty());
        categoryColumn.setCellFactory(this::makeCategoryCell);
        kindColumn.setCellValueFactory(p -> p.getValue().kindProperty());
        kindToggleGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                if (searchCriteria.getText().isEmpty())
                    filteredList.setPredicate(getPredicate());
                else
                    doSearch("name", searchCriteria.getText());
                Platform.runLater(() -> tableView.refresh());
            }
        });
        tagsColumn.setCellValueFactory(p -> p.getValue().itemProperty());
        tagsColumn.setCellFactory(this::makeTaggingCell);
        tagsColumn.prefWidthProperty()
                .bind(tableView.widthProperty()
                        .subtract(categoryColumn.widthProperty())
                        .subtract(deleteColumn.widthProperty())
                        .subtract(nameColumn.widthProperty())
                        .subtract(kindColumn.widthProperty())
                        .subtract(4.0 * 5.0));
        Platform.runLater(() -> searchCriteria.getParent().requestFocus());
        searchCriteria.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.length() >= 1 && !newValue.equals(oldValue)) {
                doSearch("name", newValue);
            } else {
                filteredList.setPredicate(getPredicate());
            }
        });
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tableView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<ItemHolder>) c -> tagButton.setDisable(c.getList().size() <= 1));
    }

    private Future<Button> makeButtonGraphic(ItemHolder itemHolder) {
        return deleteButtonLookup.computeIfAbsent(itemHolder.getItem(), key -> CompletableFuture.supplyAsync(() -> {
            Button btn = new Button();
            btn.setGraphic(SVGUtils.makeDeleteIcon());
            try {
                btn.setDisable(!model.canDeleteItem(key));
            } catch (SQLException e) {
                throw new RuntimeException("Cannot check if a item can be deleted.", e);
            }
            btn.setOnAction(event -> {
                try {
                    if (model.canDeleteItem(key)) {
                        model.deleteItem(key);
                        List<ItemHolder> found = listOfItems.stream()
                                .filter(holder -> holder.getItem().id().equals(key.id()))
                                .toList();
                        found.forEach(holder -> listOfItems.remove(holder));
                    } else {
                        userInterface.showError("Cannot delete item. Remove it from all budgets first");
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Cannot check whether it is safe to delete item or delete it");
                }
            });
            return btn;
        }));
    }

    private TableCell<ItemHolder, Item> makeCategoryCell(TableColumn<ItemHolder, Item> column) {
        TableCell<ItemHolder, Item> cell = new TableCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);
                if (!empty && item != null) {
                    Future<CategoryControlLite> future = categoryControlLookup.get(item);
                    if (future == null) {
                        future = CompletableFuture.supplyAsync(() -> makeCategoryLiteControl(item.category()));
                        categoryControlLookup.putIfAbsent(item, future);
                    }
                    try {
                        this.setGraphic(future.get());
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException("Unable to get category control from future", e);
                    }
                }
            }
        };
        cell.addEventFilter(MouseEvent.MOUSE_CLICKED, this::categoryCellEvent);
        return cell;
    }

    private CategoryControlLite makeCategoryLiteControl(Category category) {
        CategoryControlLite control = CategoryControlLite.getInstance(category);
        Tooltip tooltip = new Tooltip("Double-click to change");
        Tooltip.install(control, tooltip);
        return control;
    }

    private void categoryCellEvent(MouseEvent event) {
        if (event.getClickCount() > 1) {
            ItemHolder holder = tableView.getSelectionModel().getSelectedItem();
            Category category = holder.categoryProperty().get();
            Kind kind = holder.getKind();
            ChangeCategoryDlgController controller = ChangeCategoryDlgController
                    .getInstance(CountaryApp.OWNER_WINDOW, kind, category);
            Optional<Category> optional = controller.showAndWait();
            optional.ifPresent(c -> {
                try {
                    model.updateItemCategory(holder.getItem(), c);
                    holder.categoryProperty().set(c);
                } catch (SQLException e) {
                    throw new RuntimeException("Unable to update category for item with name " + holder.getItem().name(), e);
                }
            });
        }
    }

    private Predicate<ItemHolder> getPredicate() {
        Kind kind = (Kind) kindToggleGroup.getSelectedToggle().getUserData();
        if (kind.equals(Kind.None))
            return p -> true;
        else
            return p -> p.cat().equals(kind);
    }

    private Predicate<ItemHolder> getPredicate(Set<Long> ids) {
        Kind kind = (Kind) kindToggleGroup.getSelectedToggle().getUserData();
        if (kind.equals(Kind.None))
            return b -> ids.contains(b.getId());
        else
            return b -> b.cat().equals(kind) && ids.contains(b.getId());
    }

    public void doSearch(String what, String criteria) {
        try {
            if (!criteria.contains("*"))
                criteria = criteria + "*";
            Set<Long> items = model.searchItems(what, criteria);
            filteredList.setPredicate(getPredicate(items));
        } catch (SQLException e) {
            throw new RuntimeException("Unable to search items", e);
        }
    }

    @FXML
    public void onCreateItem() {
        CreateItemDlgController controller = CreateItemDlgController.getInstance(CountaryApp.OWNER_WINDOW, model);
        Optional<Item> optional = controller.showAndWait();
        optional.ifPresent(item -> {
            try {
                item = model.createItem(item);
                ItemHolder itemHolder = new ItemHolder(item, this::onNameChanged);
                Platform.runLater(() -> {
                    listOfItems.add(itemHolder);
                    tableView.scrollTo(itemHolder);
                });
            } catch (SQLException e) {
                DbUtils.handleException(userInterface, "item", e);
            }
        });
    }

    private TableCell<ItemHolder, Item> makeTaggingCell(TableColumn<ItemHolder, Item> col) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);
                if (!empty && item != null) {
                    Future<TaggingControl> future = taggingControlLookup.get(item);
                    if (future == null) {
                        future = CompletableFuture.supplyAsync(() -> TaggingControl.getInstance(userInterface, model, Set.of(item), tagHolderList));
                        taggingControlLookup.putIfAbsent(item, future);
                    }
                    try {
                        this.setGraphic(future.get());
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException("Unable to get TaggingControl from Future", e);
                    }
                }
            }
        };
    }

    @FXML
    private void onTag(ActionEvent actionEvent) {
        TagMultipleItemsDlgController dialog = TagMultipleItemsDlgController.getInstance(userInterface, model,
                new HashSet<>(tableView.getSelectionModel().getSelectedItems()));
        dialog.showAndWait().ifPresent(holders -> {
            initDone = false;
            taggingControlLookup = new HashMap<>();
            loadData();
            if (searchCriteria.getText().isEmpty())
                filteredList.setPredicate(getPredicate());
            else
                doSearch("name", searchCriteria.getText());
            initDone = true;
            startBackgroundTasks();
        });
    }
}
