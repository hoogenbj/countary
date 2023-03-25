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

import java.io.File;

public interface UserInterface {
    void showWarning(String content);

    String openDatabaseFile();

    File newDatabaseFile();

    File backupDatabaseToFile();

    String openBankStatementFile(String ext);

    void showError(String s);

    void showNotification(String notification);

    File restoreDatabaseFromFile();

    String chooseDB();
}
