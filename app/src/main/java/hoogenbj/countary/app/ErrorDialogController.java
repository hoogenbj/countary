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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.stage.Window;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ResourceBundle;

public class ErrorDialogController extends Dialog<String> implements Initializable {
    @FXML
    private HBox stackTraceContainer;
    @FXML
    private HBox exceptionContainer;
    @FXML
    private HBox informationContainer;
    @FXML
    private Label information;
    @FXML
    private ButtonType okButtonType;
    @FXML
    private TextArea errorMessage;
    @FXML
    private TextArea stackTrace;
    @FXML
    private ButtonType copyButtonType;

    private Button okButton;
    private Button copyButton;
    private Throwable exception;
    private String message;
    private String title;

    public static ErrorDialogController getInstance(Window owner, String message, Throwable exception) {
        ErrorDialogController controller = getInstance(owner);
        controller.exception = exception;
        controller.message = message;
        controller.title = "An unexpected error occurred";
        controller.initControls();
        return controller;
    }

    public static ErrorDialogController getInstance(Window owner, String message) {
        ErrorDialogController controller = getInstance(owner);
        controller.message = message;
        controller.title = "An error occurred";
        controller.initControls();
        return controller;
    }

    private static ErrorDialogController getInstance(Window owner) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(ErrorDialogController.class.getResource("ErrorDialog.fxml"));
        ErrorDialogController controller = null;
        try {
            DialogPane dlgPane = loader.load();
            controller = loader.getController();
            controller.initOwner(owner);
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            controller.okButton.setDefaultButton(true);
            controller.copyButton = (Button) dlgPane.lookupButton(controller.copyButtonType);
            controller.setDialogPane(dlgPane);
            controller.setResultConverter(buttonType -> "");
        } catch (IOException e) {
            throw new RuntimeException("Unable to launch ErrorDialogController", e);
        }
        return controller;
    }

    private void initControls() {
        setTitle(title);
        if (exception == null) {
            stackTraceContainer.setManaged(false);
            stackTraceContainer.setVisible(false);
            exceptionContainer.setVisible(false);
            exceptionContainer.setManaged(false);
            copyButton.setManaged(false);
            copyButton.setVisible(false);
        } else {
            errorMessage.setText(exception.getMessage());
            String st = getStackTrace(exception);
            stackTrace.setText(st);
            copyButton.setOnAction(event -> {
                ClipboardContent content = new ClipboardContent();
                content.putString(st);
                Clipboard.getSystemClipboard().setContent(content);
            });
        }
        if (message == null) {
            informationContainer.setVisible(false);
            informationContainer.setManaged(false);
        } else {
            information.setText(message);
        }
    }

    public static ErrorDialogController getInstance(Window owner, Throwable exception) {
        ErrorDialogController controller = getInstance(owner);
        controller.exception = exception;
        controller.title = "An uncaught exception occurred";
        controller.initControls();
        return controller;
    }

    private static String getStackTrace(Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        exception.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }
}
