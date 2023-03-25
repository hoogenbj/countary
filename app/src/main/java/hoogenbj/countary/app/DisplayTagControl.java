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

import hoogenbj.countary.model.DisplayTagHolder;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import java.io.IOException;

public class DisplayTagControl extends StackPane {

    @FXML
    private Text tag;

    public DisplayTagControl() {
    }

    public static DisplayTagControl getInstance(DisplayTagHolder tagHolder) {
        FXMLLoader fxmlLoader = new FXMLLoader(DisplayTagControl.class.getResource("DisplayTagControl.fxml"));
        DisplayTagControl root = new DisplayTagControl();
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(root);
        try {
            fxmlLoader.load();
            root.tag.setText(tagHolder.getName());
            return root;
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public String getTagName() {
        return tag.getText();
    }
}
