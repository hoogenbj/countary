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

package hoogenbj.countary.util;

import za.co.clock24.dsvparser.DsvParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static hoogenbj.countary.util.ParseUtils.stripQuotes;

public class Capitec_CSVStatementParser implements StatementParser {
    @Override
    public ParsedStatement parse(URI uri) throws IOException, StatementParseException {
        Reader reader = new FileReader(new File(uri));
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        final int[] lineCount = {0};
        final int[] fieldCount = {0};
        final String[] accountNumber = {null};
        DsvParser<ParsedStatement.Line> parser = new DsvParser<>(reader, fields -> {
            // skip first two lines
            if (lineCount[0] <= 1) {
                return null;
            }
            ParsedStatement.Line line = new ParsedStatement.Line();
            fieldCount[0] += 2;
            if (accountNumber[0] == null) {
                accountNumber[0] = fields[1];
            }
            fieldCount[0] += 1;
            Calendar postingDate = GregorianCalendar.from(dateFormat.parse(fields[2], LocalDate::from).atStartOfDay(ZoneId.systemDefault()));
            fieldCount[0] += 1;
            Calendar transactionDate = GregorianCalendar.from(dateFormat.parse(fields[3], LocalDate::from).atStartOfDay(ZoneId.systemDefault()));
            fieldCount[0] += 1;
            String description = fields[4];
            fieldCount[0] += 4;
            BigDecimal debitAmount = ParseUtils.parseBigDecimal(stripQuotes(fields[8]));
            fieldCount[0] += 1;
            BigDecimal creditAmount = ParseUtils.parseBigDecimal(stripQuotes(fields[9]));
            BigDecimal amount;
            if (debitAmount != null)
                amount = debitAmount.negate();
            else
                amount = creditAmount;
            fieldCount[0] += 1;
            BigDecimal balance = ParseUtils.parseBigDecimal(stripQuotes(fields[10]));
            line.setPostedOn(postingDate);
            line.setTransactionDate(transactionDate);
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
            // skip first two lines
            return lineCount[0] > 2;
        });
        ParsedStatement parsedStatement = new ParsedStatement();
        try {
            parsedStatement.setLines(parser.readAll());
            parsedStatement.setAccountNumber(accountNumber[0]);
        } catch (Exception e) {
            throw new StatementParseException(uri.getPath(), lineCount[0] + 1, fieldCount[0] % 11, e);
        }
        return parsedStatement;
    }
}
