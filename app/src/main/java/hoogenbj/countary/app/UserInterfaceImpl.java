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

import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Optional;

import static hoogenbj.countary.app.CountaryApp.OWNER_WINDOW;

public class UserInterfaceImpl implements UserInterface {
    @Override
    public void showWarning(String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING,
                content, ButtonType.CLOSE);
        alert.showAndWait();
    }

    @Override
    public String openDatabaseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a database file:");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Database Files", "*.sqlite"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File dbFile = fileChooser.showOpenDialog(OWNER_WINDOW);
        if (dbFile != null) {
            return dbFile.getAbsolutePath();
        } else
            return null;
    }

    @Override
    public File newDatabaseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("New database file:");
        fileChooser.setInitialFileName("countary.sqlite");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Database Files", "*.sqlite"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        return fileChooser.showSaveDialog(OWNER_WINDOW);
    }

    @Override
    public File restoreDatabaseFromFile(Settings settings) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Restore database from file:");
        String backupPath = settings.getBackupPath();
        if (backupPath != null)
            fileChooser.setInitialDirectory(new File(backupPath));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Database Backup Files", "*.backup"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        return fileChooser.showOpenDialog(OWNER_WINDOW);
    }

    @Override
    public File backupDatabaseToFile(Settings settings) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Backup database to file:");
        String backupPath = settings.getBackupPath();
        if (backupPath != null)
            fileChooser.setInitialDirectory(new File(backupPath));
        LocalDateTime now = LocalDateTime.now();
        String timestamp = new DateTimeFormatterBuilder()
                .appendPattern("yyyyMMddHHmmss")
                .toFormatter()
                .format(now);
        fileChooser.setInitialFileName(String.format("countary.sqlite.%s.backup", timestamp));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Database Backup Files", "*.backup"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        return fileChooser.showSaveDialog(OWNER_WINDOW);
    }

    @Override
    public String openBankStatementFile(String ext) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select bank statement file:");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Bank Statement Files", ext),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = fileChooser.showOpenDialog(OWNER_WINDOW);
        if (file != null) {
            return file.getAbsolutePath();
        } else
            return null;
    }

    @Override
    public void showError(String content) {
        ErrorDialogController instance = ErrorDialogController.getInstance(OWNER_WINDOW, content);
        instance.showAndWait();
    }

    @Override
    public String chooseDB() {
        ButtonType openDatabase = new ButtonType("Open Database", ButtonBar.ButtonData.OTHER);
        ButtonType openDemoDatabase = new ButtonType("Open Demo Database", ButtonBar.ButtonData.OTHER);
        ButtonType createDatabase = new ButtonType("Create New Database", ButtonBar.ButtonData.OTHER);
        ButtonType nothing = new ButtonType("Nothing", ButtonBar.ButtonData.OTHER);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Open Database");
        dialog.setContentText("What do you want to do?");
        dialog.getDialogPane().getButtonTypes().addAll(openDatabase, openDemoDatabase, createDatabase, nothing);
        Optional<ButtonType> selection = dialog.showAndWait();
        return selection.orElse(nothing).getText();
    }

    @Override
    public void showNotification(String notification) {
        if (OWNER_WINDOW != null && OWNER_WINDOW.isShowing())
            Notifications.create().text(notification).hideAfter(Duration.seconds(3)).showWarning();
    }
}
