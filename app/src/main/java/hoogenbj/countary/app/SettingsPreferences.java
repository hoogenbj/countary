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

import java.util.prefs.Preferences;

public class SettingsPreferences implements Settings {
    private static final String KEY_PREFIX = "/countary";
    private static final String DB_URL_KEY = KEY_PREFIX + "/database/url";
    private static final String CURRENT_ACCOUNT_KEY = KEY_PREFIX + "/current/account/key";
    private static final String CURRENT_ACCOUNT_VALUE = KEY_PREFIX + "/current/account/value";
    private static final String CUSTOM_COLORS_KEY = KEY_PREFIX + "/custom_colors";

    private final Preferences preferences;

    private String databasePath;

    private String databaseUrl;

    public SettingsPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public String getDatabaseUrl() {
        if (databaseUrl == null)
            this.databaseUrl = preferences.get(DB_URL_KEY, null);
        return databaseUrl;
    }

    @Override
    public void setDatabaseUrl(String databaseUrl) {
        setDatabaseUrl(databaseUrl, true);
    }

    @Override
    public void setDatabaseUrl(String databaseUrl, boolean remember) {
        if (remember) {
            preferences.put(DB_URL_KEY, databaseUrl);
        }
        this.databaseUrl = databaseUrl;
    }

    @Override
    public String getDatabasePath() {
        return databasePath;
    }

    @Override
    public void setDatabasePath(String databasePath) {
        this.databasePath = databasePath;
    }

    @Override
    public KeyValue getCurrentAccount() {
        String key = preferences.get(CURRENT_ACCOUNT_KEY, null);
        if (key != null) {
            String value = preferences.get(CURRENT_ACCOUNT_VALUE, null);
            if (value != null) {
                return new KeyValue(key, value);
            }
        }
        return null;
    }

    @Override
    public void setCurrentAccount(KeyValue currentAccount) {
        preferences.put(CURRENT_ACCOUNT_KEY, currentAccount.key());
        preferences.put(CURRENT_ACCOUNT_VALUE, currentAccount.value());
    }

    @Override
    public void setCustomColors(String customColors) {
        preferences.put(CUSTOM_COLORS_KEY, customColors);
    }

    @Override
    public String getCustomColors() {
        return preferences.get(CUSTOM_COLORS_KEY, "");
    }
}
