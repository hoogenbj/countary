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

import za.co.clock24.dsvparser.DsvParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static hoogenbj.countary.util.ParseUtils.stripQuotesAndWhiteSpace;

public class BankZeroStatementHistoryParser implements StatementParser {
    @Override
    public ParsedStatement parse(URI uri) throws IOException, StatementParseException {
        Reader reader = new FileReader(new File(uri));
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        final int[] lineCount = {0};
        final int[] fieldCount = {0};
        DsvParser<ParsedStatement.Line> parser = new DsvParser<>(reader, fields -> {
            // skip header line. Has to be done here also, otherwise parsing will fail
            if (lineCount[0] == 0) {
                return null;
            }
            ParsedStatement.Line line = new ParsedStatement.Line();
            Calendar postingDate = Calendar.getInstance();
            String complete=null;
            try {
                String date = fields[1];
                String time = fields[3];
                complete = date + " " + time;
                postingDate.setTime(dateFormat.parse(complete));
            } catch (ParseException e) {
                throw new RuntimeException("Unable to parse date from: " + complete, e);
            }
            String description = fields[4] + "," + fields[5] + "," + fields[6];
            BigDecimal amount = ParseUtils.parseBigDecimal(stripQuotesAndWhiteSpace(fields[8]));
            BigDecimal balance = ParseUtils.parseBigDecimal(stripQuotesAndWhiteSpace(fields[9]));
            line.setPostedOn(postingDate);
            line.setDescription(description);
            line.setBalance(balance);
            line.setAmount(amount);
            return line;
        });
        parser.setFieldCallback(field -> {
            fieldCount[0]++;
        });
        parser.setRecordCallback(line -> {
            fieldCount[0] = 0;
            lineCount[0]++;
            // skip header line
            if (lineCount[0] == 1) {
                return false;
            }
            return true;
        });
        ParsedStatement parsedStatement = new ParsedStatement();
        try {
            parsedStatement.setLines(parser.readAll());
        } catch (Exception e) {
            throw new StatementParseException(uri.getPath(), lineCount[0] + 1, fieldCount[0] % 10, e);
        }
        return parsedStatement;
    }
}
