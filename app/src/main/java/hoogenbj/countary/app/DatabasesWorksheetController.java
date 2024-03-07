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

import com.google.inject.Inject;
import hoogenbj.countary.model.DataModel;
import hoogenbj.countary.util.DbUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class DatabasesWorksheetController implements ControllerHelpers {
    @FXML
    private TextField path;
    @Inject
    private Settings settings;

    @Inject
    private DataModel model;
    @Inject
    private UserInterface userInterface;

    @FXML
    private void onOpen(ActionEvent actionEvent) {
        if (DbUtils.openDatabase(settings, userInterface)) {
            path.setText(settings.getDatabasePath());
            model.clearCache();
        }
    }

    @FXML
    private void onDemo(ActionEvent actionEvent) {
        if (DbUtils.openDemoDatabase(settings, userInterface, model)) {
            path.setText(settings.getDatabasePath());
            model.clearCache();
        }
    }

    @FXML
    private void onCreate(ActionEvent actionEvent) {
        if (DbUtils.createDatabase(settings, userInterface, model)) {
            path.setText(settings.getDatabasePath());
            model.clearCache();
        }
    }

    @FXML
    private void onBackup(ActionEvent actionEvent) {
        DbUtils.backupCurrentDatabase(model, userInterface, settings);
    }

    @FXML
    private void onRestore(ActionEvent actionEvent) {
        DbUtils.restoreFromDatabase(model, userInterface, settings);
    }

    @FXML
    private void initialize() {
        String dbUrl = settings.getDatabaseUrl();
        if (dbUrl != null) {
            path.setText(settings.getDatabasePath());
        }
    }

    @FXML
    private void onRebuildVirtualTables(ActionEvent actionEvent) {
        try {
            model.rebuildVirtualTables(userInterface);
        } catch (SQLException e) {
            throw new RuntimeException("Error rebuilding virtual tables", e);
        }
    }
}
