/*
 * Copyright (c) 2023. Johan Hoogenboezem
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
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Objects;

public class ChangeColorDlgController extends Dialog<Color> {
    @FXML
    private ColorPicker colorPicker;
    private Color selected;
    @FXML
    private ButtonType okButtonType;
    private Button okButton;

    public static ChangeColorDlgController getInstance(Window owner, Color selected) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(ChangeColorDlgController.class.getResource("ChangeColorDlg.fxml"));
        ChangeColorDlgController controller;
        try {
            DialogPane dlgPane = loader.load();
            controller = loader.getController();
            controller.setDialogPane(dlgPane);
            controller.initOwner(owner);
            controller.setTitle("Change color");
            controller.selected = selected;
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            controller.okButton.setDefaultButton(true);
            controller.initControls();
            ChangeColorDlgController finalController = controller;
            controller.setResultConverter(buttonType -> {
                if (!Objects.equals(ButtonBar.ButtonData.OK_DONE, buttonType.getButtonData())) {
                    return null;
                }
                return finalController.composeResult();
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to launch " + ChangeCategoryDlgController.class.getSimpleName(), e);
        }
        return controller;
    }

    private Color composeResult() {
        return colorPicker.getValue();
    }

    private void initControls() {
        colorPicker.setValue(selected);
    }
}
