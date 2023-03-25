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
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Window;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ResourceBundle;

public class ErrorDialogController extends Dialog<String> implements Initializable {
    @FXML
    public ButtonType okButtonType;
    public TextArea errorMessage;
    public TextArea stackTrace;

    public static ErrorDialogController getInstance(Window owner, Throwable exception) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(ErrorDialogController.class.getResource("ErrorDialog.fxml"));
        ErrorDialogController controller = null;
        try {
            DialogPane dlgPane = loader.load();
            controller = loader.getController();
            controller.initOwner(owner);
            controller.setTitle("An unexpected error occurred");
            controller.errorMessage.setText(exception.getMessage());
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            exception.printStackTrace(printWriter);
            controller.stackTrace.setText(stringWriter.toString());
            controller.setDialogPane(dlgPane);
            controller.setResultConverter(buttonType -> "");
        } catch (IOException e) {
            throw new RuntimeException("Unable to launch ErrorDialogController", e);
        }
        return controller;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
