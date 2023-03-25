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
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Predicate;

/**
 * An interactive custom control for displaying the Tags linked to an Item. Allows adding and removing of Tags
 */
public class TaggingControl extends StackPane {

    private record Coords(Integer col, Integer row) {
    }

    @FXML
    private TextField tagText;

    @FXML
    private GridPane tagGroup;

    private DataModel dataModel;

    private UserInterface userInterface;

    private final Popup popup = new Popup();
    private ObservableList<TagHolder> list;
    private FilteredList<TagHolder> filteredList;
    private final ListView<TagHolder> listView = new ListView<>();
    private final Map<TagHolder, TagControl> tags = new LinkedHashMap<>();
    private Set<Item> items;

    private Boolean isSearching = false;

    public static TaggingControl getInstance(UserInterface ui, DataModel dataModel, Set<Item> items) {
        return getInstance(ui, dataModel, items, null);
    }

    public static TaggingControl getInstance(UserInterface ui, DataModel dataModel, Set<Item> items, ObservableList<TagHolder> list) {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(TaggingControl.class.getResource("TaggingControl.fxml"));
        TaggingControl root = new TaggingControl(dataModel, ui);
        root.items = items;
        root.list = list;
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(root);
        try {
            return fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public TaggingControl() {
    }

    public TaggingControl(DataModel dataModel, UserInterface userInterface) {
        this.dataModel = dataModel;
        this.userInterface = userInterface;
    }

    @FXML
    private void onKeyTyped(KeyEvent keyEvent) {
        if (!keyEvent.getCharacter().equals(KeyEvent.CHAR_UNDEFINED)) {
            isSearching = true;
        } else {
            isSearching = false;
        }
    }

    /**
     * If the enter key is detected, either create a new tag or select an existing one and add it to the display
     * as well as the list of tags linked to the item.
     *
     * @param keyEvent
     */
    @FXML
    private void onKeyReleased(KeyEvent keyEvent) {
        switch (keyEvent.getCode()) {
            case ENTER -> {
                String tagName = tagText.getText().trim();
                if (tagName.isEmpty()) {
                    keyEvent.consume();
                    return;
                }
                try {
                    Tag tag = dataModel.getTag(tagName);
                    if (tag == null) {
                        tag = dataModel.createTag(tagName);
                    } else {
                        Tag finalTag = tag;
                        if (tags.keySet().stream().anyMatch(tagHolder -> tagHolder.name().equals(finalTag.name()))) {
                            return;
                        }
                    }
                    Tag finalTag = tag;
                    items.forEach((item -> {
                        try {
                            dataModel.createTagForItemIfNotExist(item, finalTag);
                        } catch (SQLException e) {
                            throw new RuntimeException("Unable to create item-tag", e);
                        }
                    }));
                    TagHolder tagHolder = TagHolder.of(items, tag);
                    addToTagGroup(tagHolder);
                    list.add(tagHolder);
                    tagText.clear();
                } catch (SQLException e) {
                    userInterface.showError("Error with tag: " + tagText.getText());
                }
            }
            case ESCAPE -> {
                if (popup.isShowing())
                    popup.hide();
            }
        }
    }

    /**
     * When a tag is closed by clicking on the 'x', remove it from the display and from the item's collection of
     * tags in the database.
     *
     * @param tagControl
     */
    private void closeTag(TagControl tagControl) {
        TagHolder tagHolder = (TagHolder) tagControl.getUserData();
        tagHolder.getItems().forEach((item -> {
            try {
                dataModel.deleteTagForItem(item, tagHolder.getId());
            } catch (SQLException e) {
                throw new RuntimeException("Unable to remove tag from item", e);
            }
        }));
        Coords removed = null;
        // Fill gap left in grid pane when tag is removed
        for (Iterator<TagHolder> it = tags.keySet().iterator(); it.hasNext(); ) {
            TagHolder t = it.next();
            if (removed != null) {
                TagControl control = tags.get(t);
                Coords current = new Coords(GridPane.getColumnIndex(control), GridPane.getRowIndex(control));
                GridPane.setConstraints(control, removed.col(), removed.row());
                removed = current;
            }
            if (t.equals(tagHolder)) {
                removed = new Coords(GridPane.getColumnIndex(tagControl), GridPane.getRowIndex(tagControl));
                tagGroup.getChildren().remove(tagControl);
                it.remove();
            }
        }
    }

    private Predicate<TagHolder> getPredicate(Set<Long> ids) {
        return t -> ids.contains(t.getId());
    }

    public void doSearch(String what, String criteria) {
        try {
            Set<Long> tagIds = dataModel.searchTags(what, String.format("%s*", criteria));
            filteredList.setPredicate(getPredicate(tagIds));
        } catch (SQLException e) {
            throw new RuntimeException("Unable to search tags", e);
        }
    }

    public void initialize() {
        try {
            // Find the tags common to all the items
            Set<Tag> intersection = new HashSet<>();
            boolean first = true;
            for (Item item : items) {
                try {
                    Set<Tag> tags = dataModel.getTagsForItem(item);
                    if (first) {
                        first = false;
                        intersection.addAll(tags);
                    } else {
                        intersection.retainAll(tags);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("Unable to retrieve tags for item");
                }
            }
            intersection.forEach((tag -> {
                addToTagGroup(TagHolder.of(items, tag));
            }));
            if (list == null)
                list = FXCollections.observableArrayList(dataModel.getTags());
        } catch (SQLException e) {
            throw new RuntimeException("Could not retrieve tags", e);
        }
        filteredList = new FilteredList<>(list);
        filteredList.addListener(this::showExistingTags);
        SortedList<TagHolder> sortedList = new SortedList<>(filteredList, Comparator.comparing(TagHolder::name));
        listView.setPrefHeight(120.0);
        listView.setItems(sortedList);
        listView.getSelectionModel().selectedItemProperty().addListener(this::addSelectedTag);
        popup.getContent().add(listView);
        tagText.textProperty().addListener(this::searchOnChange);
    }

    /**
     * When letters are entered in the field, do a search for existing tags. If field is emptied, cancel the search
     *
     * @param observable
     * @param oldValue
     * @param newValue
     */
    private void searchOnChange(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        if (newValue != null && !newValue.equals(oldValue)) {
            if (!newValue.trim().isEmpty()) {
                doSearch("name", newValue.trim());
            } else {
                filteredList.setPredicate(tagHolder -> false);
            }
        }
    }

    /**
     * If one of the displayed tags is selected, add it to the item's tags and to the display
     *
     * @param observable
     * @param oldValue
     * @param newValue
     */
    private void addSelectedTag(ObservableValue<? extends TagHolder> observable, TagHolder oldValue, TagHolder newValue) {
        if (newValue != null && !newValue.equals(oldValue)) {
            if (!tags.containsKey(newValue)) {
                try {
                    Tag tag = dataModel.getTag(newValue.name());
                    items.forEach((item -> {
                        try {
                            dataModel.createTagForItemIfNotExist(item, tag);
                        } catch (SQLException e) {
                            throw new RuntimeException("Could not create item-tag", e);
                        }
                    }));
                    newValue.setItems(items);
                    addToTagGroup(newValue);
                } catch (SQLException e) {
                    throw new RuntimeException("Could not retrieve tag from database", e);
                }
            }
            Platform.runLater(() -> tagText.clear());
            if (popup.isShowing())
                popup.hide();
        }
    }

    /**
     * Add a TagControl to the display
     *
     * @param tagHolder
     */
    private void addToTagGroup(TagHolder tagHolder) {
        int rowCount = tags.size() / 3;
        int columnCount = tags.size() % 3;
        TagControl tagControl = TagControl.getInstance(tagHolder, this::closeTag);
        tagControl.setUserData(tagHolder);
        tagGroup.add(tagControl, columnCount, rowCount);
        tags.put(tagHolder, tagControl);
    }

    /**
     * Displays a popup list of tags that matched the letters in the search field.
     *
     * @param c
     */
    private void showExistingTags(ListChangeListener.Change<? extends TagHolder> c) {
        if (!isSearching)
            return;
        if (c.getList().size() > 0) {
            if (!popup.isShowing()) {
                Bounds bounds = tagText.localToScreen(tagText.getBoundsInLocal());
                popup.show(tagGroup.getScene().getWindow(), bounds.getMinX(), bounds.getMaxY());
            }
        } else {
            if (popup.isShowing())
                popup.hide();
        }
    }
}
