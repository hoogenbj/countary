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

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TagToggle extends StackPane {

    private Consumer<TagToggle> callback;
    @FXML
    private ToggleButton toggleButton;
    @FXML
    private Text tag;

    public TagToggle() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("TagToggle.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        fxmlLoader.setClassLoader(getClass().getClassLoader());
        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException("Unable to load TagToggle", exception);
        }
    }

    public void setCallback(BiConsumer<TagToggle, Boolean> callback) {
        toggleButton.setOnMouseClicked(action -> callback.accept(this, action.isShiftDown()));
    }

    public String getTagName() {
        return tag.getText();
    }

    public void setTagName(String tagName) {
        this.tag.setText(tagName);
    }

    public ToggleButton getToggleButton() {
        return toggleButton;
    }
}
