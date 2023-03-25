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

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RMBPB_CSVStatementParser implements StatementParser {
    private final Pattern txDatePattern = Pattern.compile("(\\d{2} (JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))$");

    @Override
    public ParsedStatement parse(URI uri) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(uri));
        if (!lines.get(0).equals("ACCOUNT TRANSACTION HISTORY")) {
            throw new RuntimeException("First line of file should read: \"ACCOUNT TRANSACTION HISTORY\". Wrong file?");
        }
        ParsedStatement parsedStatement = new ParsedStatement();
        List<ParsedStatement.Line> statementLines = new ArrayList<>();
        parsedStatement.setLines(statementLines);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        DateFormat txDateFormat = new SimpleDateFormat("dd MMM yyyy");
        int lineCount = 0;
        try {
            for (String line : lines) {
                switch (lineCount) {
                    case 0:
                    case 1:
                        // ignore first two lines.
                        break;
                    case 2: {
                        String[] fields = line.split(",");
                        parsedStatement.setAccountHolder(fields[1].trim()+" "+fields[2].trim());
                    }
                    case 3: {
                        String[] fields = line.split(",");
                        parsedStatement.setAccountNumber(fields[1].trim());
                    }
                    case 4:
                    case 5:
                    case 6:
                        // ignore these lines
                        break;
                    default: {
                        ParsedStatement.Line l = new ParsedStatement.Line();
                        String[] fields = line.split(",");
                        Calendar postingDate = Calendar.getInstance();
                        postingDate.setTime(dateFormat.parse(fields[0].trim()));
                        l.setPostedOn(postingDate);
                        l.setAmount(new BigDecimal(fields[1].trim()));
                        l.setBalance(new BigDecimal(fields[2].trim()));
                        l.setDescription(fields[3].trim());
                        Matcher matcher = txDatePattern.matcher(fields[3]);
                        if (matcher.find()) {
                            String dateField = matcher.group(1);
                            String yearPortion = String.valueOf(postingDate.get(Calendar.YEAR));
                            Calendar txDate = Calendar.getInstance();
                            txDate.setTime(txDateFormat.parse(dateField + " " + yearPortion));
                            if (postingDate.get(Calendar.MONTH) == Calendar.JANUARY && txDate.get(Calendar.MONTH) == Calendar.DECEMBER) {
                                // transaction date is in the previous year
                                txDate.set(Calendar.YEAR, postingDate.get(Calendar.YEAR)-1);
                            }
                            l.setTransactionDate(txDate);
                        }
                        statementLines.add(l);
                    }
                }
                lineCount++;
            }
        } catch (Exception e) {
            throw new StatementParseException(uri.getPath(), lineCount + 1, e);
        }
        return parsedStatement;
    }
}
