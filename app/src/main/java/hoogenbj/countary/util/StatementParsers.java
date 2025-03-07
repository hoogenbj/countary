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

package hoogenbj.countary.util;

public enum StatementParsers {
    RMBPBCSV("CSV (RMB Private Bank or FNB)", RMBPB_CSVStatementParser.class, "*.csv"),
    BZCSV("CSV (Bank Zero monthly)", BankZeroStatementParser.class, "*.csv"),
    BZHCSV("CSV (Bank Zero history)", BankZeroStatementHistoryParser.class, "*.csv"),
    OFX1("OFX v1.x", OFXStatementParser.class, "*.ofx"),
    OFX2("OFX v2.x", OFX2StatementParser.class, "*.ofx");

    private final String description;
    private final Class<? extends StatementParser> parser;
    private final String fileExt;

    StatementParsers(String description, Class<? extends StatementParser> parser, String fileExt) {
        this.description = description;
        this.parser = parser;
        this.fileExt = fileExt;
    }

    public String description() {
        return description;
    }

    public String fileExt() {
        return fileExt;
    }

    public Class<? extends StatementParser> parser() {
        return parser;
    }
}
