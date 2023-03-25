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

import hoogenbj.countary.model.BudgetTagProfile;
import hoogenbj.countary.util.ParseUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BudgetTagProfileControl extends VBox {
    @FXML
    private TagToggle tagToggle;
    @FXML
    private TextField totalPlanned;
    @FXML
    private TextField totalActual;
    private BudgetTagProfile profile;
    private BiConsumer<TagToggle, Boolean> callback;

    public BudgetTagProfileControl() {
    }

    public static BudgetTagProfileControl getInstance(BudgetTagProfile profile, BiConsumer<TagToggle, Boolean> callback) {
        FXMLLoader fxmlLoader = new FXMLLoader(BudgetTagProfileControl.class.getResource("BudgetTagProfile.fxml"));
        BudgetTagProfileControl root = new BudgetTagProfileControl();
        root.profile = profile;
        root.callback = callback;
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(root);
        try {
            fxmlLoader.load();
            return root;
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void initialize() {
        totalPlanned.setText(profile.getTotalPlanned());
        totalActual.setText(profile.getTotalActual());
        tagToggle.setCallback(callback);
        tagToggle.setTagName(profile.getTagName());
        profile.totalActualProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                totalActual.setText(newValue);
            }
        });
        profile.totalPlannedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                totalPlanned.setText(newValue);
            }
        });
    }

}
