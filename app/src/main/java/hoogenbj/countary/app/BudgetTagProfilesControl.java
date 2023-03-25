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
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class BudgetTagProfilesControl extends VBox {

    public BudgetTagProfilesControl() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("BudgetTagProfiles.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.setClassLoader(getClass().getClassLoader());
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setProfiles(List<List<BudgetTagProfile>> profiles, BiConsumer<TagToggle, Boolean> callback) {
        this.getChildren().clear();
        profiles.forEach(list -> {
            HBox row = new HBox();
            list.forEach(p -> {
                BudgetTagProfileControl profileControl = BudgetTagProfileControl.getInstance(p, callback);
                row.getChildren().add(profileControl);
            });
            this.getChildren().add(row);
        });
        this.layout();
    }
}
