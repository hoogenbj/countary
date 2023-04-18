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

import hoogenbj.countary.util.StatementParsers;

public interface Settings {
    String getDatabaseUrl();

    void setDatabaseUrl(String databaseUrl);

    void setDatabaseUrl(String databaseUrl, boolean remember);

    String getDatabasePath();

    void setDatabasePath(String databasePath);

    KeyValue getCurrentAccount();

    void setCurrentAccount(KeyValue currentAccount);

    StatementParsers getAccountStatement(int hashcode);

    void setCustomColors(String customColors);

    String getCustomColors();

    void setAccountStatement(int hashcode, StatementParsers parser);
}
