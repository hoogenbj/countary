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

import com.google.inject.Guice;
import com.google.inject.Injector;
import hoogenbj.countary.di.GuiceModule;
import hoogenbj.countary.model.DataModel;
import hoogenbj.countary.util.DbUtils;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.InputStream;
import java.sql.SQLException;

public class CountaryApp extends Application implements Thread.UncaughtExceptionHandler {

    private static final int DBVERSION = 2;
    public static Window OWNER_WINDOW = null;
    public static Injector injector;

    @Override
    public void init() throws Exception {
        super.init();
        injector = Guice.createInjector(new GuiceModule());
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void start(Stage stage) {
        UserInterface userInterface = injector.getInstance(UserInterface.class);
        DataModel model = injector.getInstance(DataModel.class);
        try {
            if (!dbInitialised(injector)) {
                userInterface.showWarning("Unfortunately it is not possible to continue without a database");
                return;
            }
            int currentDbVersion = currentDatabaseVersion(model);
            if (dbMigrationNeeded(currentDbVersion)) {
                userInterface.showWarning(String.format("The database needs to be migrated from version %d to %d",
                        currentDbVersion, currentDbVersion+1));
                DatabaseMigrationController.getInstance(injector, stage, currentDbVersion);
            } else {
                new CountaryController().show(injector, stage);
            }
            InputStream resource = getClass().getResourceAsStream("countary.png");
            if (resource != null)
                stage.getIcons().add(new Image(resource));
            else
                throw new RuntimeException("Could not find resource: countary.png");
        } catch (Throwable e) {
            showError(e);
        }
    }

    private boolean dbMigrationNeeded(int currentDbVersion) {
        return currentDbVersion != DBVERSION;
    }

    private boolean dbInitialised(Injector injector) throws SQLException {
        Settings settings = injector.getInstance(Settings.class);
        DataModel model = injector.getInstance(DataModel.class);
        UserInterface userInterface = injector.getInstance(UserInterface.class);
        String databaseUrl = getOrSetDatabaseUrl(model, settings, userInterface);
        return databaseUrl != null;
    }

    private int currentDatabaseVersion(DataModel model) throws SQLException {
        int dbVersion;
        if (!model.tableExists("db_version"))
            dbVersion = 1;
        else {
            dbVersion = model.getDbVersion();
        }
        return dbVersion;
    }

    private String getOrSetDatabaseUrl(DataModel model, Settings settings, UserInterface userInterface) {
        String dbUrl = settings.getDatabaseUrl();
        if (!DbUtils.validUrl(dbUrl)) {
            String choice = userInterface.chooseDB();
            switch (choice) {
                case "Open Database" -> DbUtils.openDatabase(settings, userInterface);
                case "Open Demo Database" -> DbUtils.openDemoDatabase(settings, userInterface, model);
                case "Create New Database" -> DbUtils.createDatabase(settings, userInterface, model);
                default -> {
                }
            }
            return settings.getDatabaseUrl();
        } else {
            settings.setDatabasePath(dbUrl.substring(dbUrl.lastIndexOf(":") + 1));
        }
        return dbUrl;
    }


    public static void main(String[] args) {
        launch();
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        showError(e);
    }

    private static void showError(Throwable e) {
        ErrorDialogController instance = ErrorDialogController.getInstance(OWNER_WINDOW, e);
        instance.showAndWait();
    }
}
