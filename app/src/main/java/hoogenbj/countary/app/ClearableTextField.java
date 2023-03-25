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

import javafx.animation.FadeTransition;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;

public class ClearableTextField extends StackPane {
    @FXML
    private StackPane clearButtonContainer;
    @FXML
    private TextField textField;

    public void setPromptText(String promptText) {
        textField.setPromptText(promptText);
    }

    public String getPromptText() {
        return textField.getPromptText();
    }

    public ClearableTextField() {
        FXMLLoader fxmlLoader = new FXMLLoader(DisplayTagControl.class.getResource("ClearableTextField.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);
        try {
            fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load ClearableTextField", e);
        }
    }

    public StringProperty textProperty() {
        return textField.textProperty();
    }

    @FXML
    private void initialize() {
        final FadeTransition fadeTransition = new FadeTransition(Duration.millis(300), clearButtonContainer);
        fadeTransition.setCycleCount(1);
        clearButtonContainer.setOnMouseReleased(e -> textField.clear());
        textField.textProperty().addListener(new InvalidationListener() {
            private boolean isVisible = false;
            @Override
            public void invalidated(Observable observable) {
                String text = textField.getText();
                boolean empty = text == null || text.length() == 0;

                if (empty == isVisible) {
                    isVisible = !empty;
                    fadeTo(isVisible);
                }
            }
            private void fadeTo(boolean visible) {
                fadeTransition.stop();
                fadeTransition.setFromValue(visible ? 0.0: 1.0);
                fadeTransition.setToValue(visible ? 1.0: 0.0);
                fadeTransition.play();
            }
        });
    }

    public void setText(String s) {
        textField.setText(s);
    }

    public String getText() {
        return textField.getText();
    }
}
