/*
 * Copyright (c) 2022-2023. Johan Hoogenboezem
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

package hoogenbj.countary.util;

import hoogenbj.countary.app.Settings;
import hoogenbj.countary.app.UserInterface;
import hoogenbj.countary.model.DataModel;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import static org.sqlite.SQLiteErrorCode.*;

public class DbUtils {

    public static Integer MAX_TRANSACTION_ROWS = 2000;

    public static boolean dbFileNotFound(String dbUrl) {
        String[] parts = dbUrl.split(":");
        return !new File(parts[parts.length - 1]).exists();
    }

    public static boolean validUrl(String dbUrl) {
        if (dbUrl == null)
            return false;
        else
            return !dbFileNotFound(dbUrl);
    }

    public static boolean openDatabase(Settings settings, UserInterface userInterface) {
        String filePath = userInterface.openDatabaseFile();
        if (filePath != null) {
            String dbUrl = String.format("jdbc:sqlite:%s", filePath);
            settings.setDatabaseUrl(dbUrl);
            settings.setDatabasePath(filePath);
            userInterface.showNotification(String.format("Opening of database %s completed.", filePath));
            return true;
        }
        return false;
    }

    public static boolean openDemoDatabase(Settings settings, UserInterface userInterface, DataModel model) {
        backupDatabaseSettings(settings);
        File tempFile = null;
        try {
            tempFile = File.createTempFile(String.format("countary_demo_%d", System.currentTimeMillis()), ".sqlite.tmp");
            tempFile.deleteOnExit();
            String dbUrl = String.format("jdbc:sqlite:%s", tempFile);
            settings.setDatabaseUrl(dbUrl, false);
            settings.setDatabasePath(tempFile.getAbsolutePath());
            List<String> meta = IOUtils.readLines(Objects
                    .requireNonNull(DbUtils.class.getResourceAsStream("database.sql")), (Charset) null);
            List<String> data = IOUtils.readLines(Objects
                    .requireNonNull(DbUtils.class.getResourceAsStream("demo.sql")), (Charset) null);
            model.createDemoDatabase(meta, data);
            userInterface.showNotification(String.format("Creation of demo database %s completed.", tempFile.getAbsolutePath()));
            return true;
        } catch (IOException e) {
            userInterface.showError(String.format("Unable to initialise demo database due to %s", e));
            restoreDatabaseSettings(settings);
        } catch (SQLException e) {
            userInterface.showError(String.format("Unable to execute statements to initialise demo database in %s due to %s",
                    tempFile.getAbsolutePath(), e));
            restoreDatabaseSettings(settings);
        } catch (Throwable e) {
            if (tempFile != null)
                userInterface.showError(String.format("Unexpected error while trying to create a demo database in %s due to %s",
                        tempFile.getAbsolutePath(), e));
            else
                userInterface.showError(String.format("Unexpected error while trying to create a demo database due to %s", e));
            restoreDatabaseSettings(settings);
        }
        return false;
    }

    private static String previousDbUrl = null;
    private static String previousFilePath = null;

    private static void backupDatabaseSettings(Settings settings) {
        previousDbUrl = settings.getDatabaseUrl();
        previousFilePath = settings.getDatabasePath();
    }

    private static void restoreDatabaseSettings(Settings settings) {
        if (previousDbUrl != null) {
            settings.setDatabasePath(previousFilePath);
            settings.setDatabaseUrl(previousDbUrl);
        }
    }

    public static boolean createDatabase(Settings settings, UserInterface userInterface, DataModel model) {
        File file = userInterface.newDatabaseFile();
        if (file != null) {
            String filePath = file.getAbsolutePath();
            String dbUrl = String.format("jdbc:sqlite:%s", filePath);
            backupDatabaseSettings(settings);
            settings.setDatabaseUrl(dbUrl);
            settings.setDatabasePath(filePath);
            try {
                List<String> statements = IOUtils.readLines(Objects
                        .requireNonNull(DbUtils.class.getResourceAsStream("database.sql")), (Charset) null);
                model.executeStatements(statements);
                userInterface.showNotification(String.format("Creation of database %s completed.", file.getAbsolutePath()));
                return true;
            } catch (SQLException e) {
                userInterface.showError(String.format("Unable to execute statements to initialise database at %s due to %s", filePath, e));
                restoreDatabaseSettings(settings);
            } catch (Throwable e) {
                userInterface.showError(String.format("Unexpected error while trying to create a database file at %s due to %s", filePath, e));
                restoreDatabaseSettings(settings);
            }
        }
        return false;
    }

    public static String backupCurrentDatabase(DataModel model, UserInterface userInterface, Settings settings) {
        File file = userInterface.backupDatabaseToFile(settings);
        if (file != null) {
            try {
                model.backup(file.getAbsolutePath());
                userInterface.showNotification(String.format("Backup to %s completed.", file.getAbsolutePath()));
                settings.setBackupPath(file.getParentFile().getAbsolutePath());
                return file.getAbsolutePath();
            } catch (SQLException e) {
                userInterface.showError("Unable to backup database because of " + e);
            }
        }
        return null;
    }

    public static String restoreFromDatabase(DataModel model, UserInterface userInterface, Settings settings) {
        File file = userInterface.restoreDatabaseFromFile(settings);
        if (file != null) {
            try {
                model.restore(file.getAbsolutePath());
                userInterface.showNotification(String.format("Restore from %s completed.", file.getAbsolutePath()));
                return file.getAbsolutePath();
            } catch (SQLException e) {
                userInterface.showError("Unable to restore database because of " + e);
            }
        }
        return null;
    }

    public static void handleException(UserInterface userInterface, String record, SQLException e) {
        if (e.getErrorCode() == SQLITE_CONSTRAINT.code) {
            userInterface.showError(String.format("A %s with that name already exists", record));
        } else if (e.getErrorCode() == SQLITE_CORRUPT_VTAB.code) {
            userInterface.showError(String.format("%s. Rebuild virtual tables", SQLITE_CORRUPT_VTAB.message));
        } else {
            throw new RuntimeException(String.format("Unable to create %s", record), e);
        }

    }
}
