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
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

public class BudgetSummaryController {

    @FXML
    private HBox container;
    private final UserInterface userInterface;
    private final DataModel model;
    private final Budget budget;
    private final Map<String, Boolean> tagLookup = new HashMap<>();
    private final Map<String, BudgetTagProfile> profileMap = new HashMap<>();
    private final FilteredList<BudgetItemHolder> filteredBudgetItemList;
    private final CategoryModel categoryModel;

    public BudgetSummaryController(UserInterface userInterface, DataModel model, Budget budget,
                                   FilteredList<BudgetItemHolder> filteredBudgetItemList,
                                   CategoryModel categoryModel) {
        this.userInterface = userInterface;
        this.categoryModel = categoryModel;
        this.model = model;
        this.budget = budget;
        this.filteredBudgetItemList = filteredBudgetItemList;
    }

    public Map<String, BudgetTagProfile> getProfileMap() {
        return profileMap;
    }

    public Node createNode() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(this.getClass().getResource("BudgetSummary.fxml"));
        loader.setController(this);
        return loader.load();
    }

    @FXML
    private void initialize() {
        initControls();
    }

    private void initControls() {
        BudgetTagProfilesControl profilesControl = new BudgetTagProfilesControl();
        BudgetModel budgetModel = new BudgetModel(model);
        try {
            List<List<BudgetTagProfile>> profile = budgetModel.getBudgetTagProfile(budget);
            profile.stream().flatMap(Collection::stream).forEach(p -> {
                tagLookup.put(p.getTagName(), true);
                profileMap.put(p.getTagName(), p);
            });
            profilesControl.setProfiles(profile, (tagToggle, shiftIsDown) -> {
                // TODO: do something else when shiftIsDown
                tagLookup.put(tagToggle.getTagName(), tagToggle.getToggleButton().isSelected());
                filteredBudgetItemList.setPredicate(p -> {
                    Set<Tag> tags = p.getBudgetItem().tags();
                    boolean on = true;
                    for (Tag tag : tags) {
                        if (!tagLookup.get(tag.name())) {
                            on = false;
                            break;
                        }
                    }
                    return on;
                });
            });
            CategoryController categoryController = new CategoryController(model, budget.kind(),
                    categoryModel);
            Node categoryControl = categoryController.createNode();
            ScrollPane categoryScrollPane = new ScrollPane(categoryControl);
            categoryScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            categoryScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            HBox.setHgrow(categoryScrollPane, Priority.ALWAYS);
            if (profile.isEmpty()) {
                Platform.runLater(() -> container.getChildren().add(categoryScrollPane));
            } else {
                Platform.runLater(() -> {
                    ScrollPane profilesScrollPane = new ScrollPane(profilesControl);
                    profilesScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                    profilesScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    HBox.setHgrow(profilesScrollPane, Priority.NEVER);
                    container.getChildren().addAll(profilesScrollPane, categoryScrollPane);
                    categoryControl.prefWidth(container.getWidth() - profilesControl.getWidth());
                });
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to load budget tag profiles", e);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load Category control", e);
        }
    }
}
