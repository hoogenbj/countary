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

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static hoogenbj.countary.util.ParseUtils.stripQuotesAndWhiteSpace;
import static org.junit.jupiter.api.Assertions.*;

public class StatementParserTest {

    @Test
    public void parseCapitecCsvTest() throws Exception {
        ParsedStatement parsedStatement = new Capitec_CSVStatementParser()
                .parse(this.getClass().getResource("CapitecBankTransactionHistory_123-123.csv").toURI());
        assertEquals("1234567890", parsedStatement.getAccountNumber());
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Calendar expectedDate = GregorianCalendar.from(dateFormat.parse("18/04/2023", LocalDate::from)
                .atStartOfDay(ZoneId.systemDefault()));
        expectedDate.add(Calendar.SECOND, 23);
        int checked = 0;
        assertEquals(32, parsedStatement.getLines().size());
        for (ParsedStatement.Line line :
                parsedStatement.getLines()) {
            if (expectedDate.getTime().equals(line.getPostedOn().getTime())) {
                assertEquals("Total Johannesburg (Card 1234)", line.getDescription());
                assertEquals(GregorianCalendar.from(dateFormat.parse("14/04/2023", LocalDate::from)
                        .atStartOfDay(ZoneId.systemDefault())).getTime(), line.getTransactionDate().getTime());
                assertEquals(new BigDecimal("-65.90"), line.getAmount());
                assertEquals(new BigDecimal("17144.59"), line.getBalance());
                checked++;
            }
        }
        assertEquals(1, checked);
    }

    @Test
    public void parseCapitecCsvSortOrderTest() throws Exception {
        ParsedStatement parsedStatement = new Capitec_CSVStatementParser()
                .parse(this.getClass().getResource("CapitecBankTransactionHistory_123-123.csv").toURI());
        List<ParsedStatement.Line> lines = parsedStatement.getLines();
        lines.sort(Comparator.comparing(item -> item.getPostedOn().getTime()));
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Calendar expectedDate = GregorianCalendar.from(dateFormat.parse("01/05/2023", LocalDate::from)
                .atStartOfDay(ZoneId.systemDefault()));
        expectedDate.add(Calendar.SECOND, 36);
        System.out.println(lines.get(27).getPostedOn().getTime());
        assertEquals(expectedDate.getTime(), lines.get(27).getPostedOn().getTime());
        expectedDate.add(Calendar.SECOND, 1);
        assertEquals(expectedDate.getTime(), lines.get(28).getPostedOn().getTime());
        expectedDate.add(Calendar.SECOND, 1);
        assertEquals(expectedDate.getTime(), lines.get(29).getPostedOn().getTime());
        expectedDate.add(Calendar.SECOND, 1);
        assertEquals(expectedDate.getTime(), lines.get(30).getPostedOn().getTime());
    }

    @Test
    public void testHash() throws Exception {
        String[] fields = List.of("3", "1234567890", "08/04/2023", "05/04/2023", "Cx Roodepoort (Card 1234)",
                        "Cx Roodepoort         Roodepoort   ZA", "Food", "Groceries", "63.97", "", "20151.53")
                .toArray(new String[0]);
        ParsedStatement.Line line1 = parseLine(fields);
        Thread.sleep(2000);
        ParsedStatement.Line line2 = parseLine(fields);
        System.out.println(line2.hashCode());
        assertEquals(line2.hashCode(), line1.hashCode());
    }

    private static ParsedStatement.Line parseLine(String[] fields) {
        final int[] fieldCount = {0};
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        ParsedStatement.Line line = new ParsedStatement.Line();
        fieldCount[0] += 2;
        fieldCount[0] += 1;
        Calendar postingDate = GregorianCalendar.from(dateFormat.parse(fields[2], LocalDate::from)
                .atStartOfDay(ZoneId.systemDefault()));
        fieldCount[0] += 1;
        Calendar transactionDate = GregorianCalendar.from(dateFormat.parse(fields[3], LocalDate::from)
                .atStartOfDay(ZoneId.systemDefault()));
        fieldCount[0] += 1;
        String description = fields[4];
        fieldCount[0] += 4;
        BigDecimal debitAmount = ParseUtils.parseBigDecimal(stripQuotesAndWhiteSpace(fields[8]));
        fieldCount[0] += 1;
        BigDecimal creditAmount = ParseUtils.parseBigDecimal(stripQuotesAndWhiteSpace(fields[9]));
        BigDecimal amount;
        if (debitAmount != null)
            amount = debitAmount.negate();
        else
            amount = creditAmount;
        fieldCount[0] += 1;
        BigDecimal balance = ParseUtils.parseBigDecimal(stripQuotesAndWhiteSpace(fields[10]));
        line.setPostedOn(postingDate);
        line.setTransactionDate(transactionDate);
        line.setDescription(description);
        line.setBalance(balance);
        line.setAmount(amount);
        return line;
    }

    @Test
    public void parseRMBPBTest() throws Exception {
        ParsedStatement parsedStatement = new RMBPB_CSVStatementParser()
                .parse(this.getClass().getResource("rmbpb_account.csv").toURI());
        assertEquals("Joe Soap", parsedStatement.getAccountHolder());
        assertEquals("123412341234", parsedStatement.getAccountNumber());
        assertEquals(17, parsedStatement.getLines().size());
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        DateFormat txDateFormat = new SimpleDateFormat("dd MMM yyyy");
        int checked = 0;
        for (ParsedStatement.Line line : parsedStatement.getLines()) {
            if (dateFormat.parse("2021/12/11").equals(line.getPostedOn().getTime())) {
                assertEquals("Pharmacy 470720*1112  08 DEC", line.getDescription());
                assertEquals(txDateFormat.parse("08 DEC 2021"), line.getTransactionDate().getTime());
                assertEquals(new BigDecimal("-534.63"), line.getAmount());
                assertEquals(new BigDecimal("8081.49"), line.getBalance());
                checked++;
            }
        }
        assertEquals(1, checked);
    }

    @Test
    public void parseBankZeroTest() throws Exception {
        ParsedStatement parsedStatement = new BankZeroStatementParser().parse(this.getClass()
                .getResource("bankzero_account.csv").toURI());
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        for (ParsedStatement.Line line : parsedStatement.getLines()) {
            if (dateFormat.parse("04/12/2021 11:55").equals(line.getPostedOn().getTime())) {
                assertEquals("Pay in,Capitec bank limited,CAPITEC  JANE", line.getDescription());
                assertEquals(new BigDecimal("600.00"), line.getAmount());
                assertEquals(new BigDecimal("1536.00"), line.getBalance());
            } else if (dateFormat.parse("27/12/2021 11:44").equals(line.getPostedOn().getTime())) {
                assertEquals("Card purchase,Exclusive Books Sandton,Sandton Dip Transaction",
                        line.getDescription());
                assertEquals(new BigDecimal("-206.00"), line.getAmount());
                assertEquals(ParseUtils.parseBigDecimal("1,330.00"), line.getBalance());
            } else if (dateFormat.parse("31/12/2021 18:38").equals(line.getPostedOn().getTime())) {
                assertEquals("Card purchase,Loot Online,Johannesburg Online Transaction",
                        line.getDescription());
                assertEquals(ParseUtils.parseBigDecimal("-1,189.00"), line.getAmount());
                assertEquals(new BigDecimal("141.00"), line.getBalance());
            } else {
                fail("Should not get here");
            }
        }
    }

    @Test
    public void parseBankZeroHistoryTest() throws Exception {
        ParsedStatement parsedStatement = new BankZeroStatementHistoryParser().parse(this.getClass()
                .getResource("bankzero_account_history.csv").toURI());
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        for (ParsedStatement.Line line : parsedStatement.getLines()) {
            System.out.println(line);
            if (dateFormat.parse("09/10/2023 18:17").equals(line.getPostedOn().getTime())) {
                assertEquals("Payment In,Firstrand bank,SINGLE FACILITY", line.getDescription());
                assertEquals(new BigDecimal("1848.55"), line.getAmount());
                assertEquals(new BigDecimal("6054.97"), line.getBalance());
            } else if (dateFormat.parse("01/10/2023 14:03").equals(line.getPostedOn().getTime())) {
                assertEquals("Card Purchase,Apple.com/bill, Ireland,44.99 ZAR (@ rate R1.00), Online transaction",
                        line.getDescription());
                assertEquals(new BigDecimal("-44.99"), line.getAmount());
                assertEquals(ParseUtils.parseBigDecimal("3108.42"), line.getBalance());
            } else if (dateFormat.parse("01/10/2023 03:51").equals(line.getPostedOn().getTime())) {
                assertEquals("Card Purchase,Netflix.com, Netherlands,159.00 ZAR (@ rate R1.00), Online transaction",
                        line.getDescription());
                assertEquals(ParseUtils.parseBigDecimal("-159.00"), line.getAmount());
                assertEquals(new BigDecimal("3153.41"), line.getBalance());
            } else {
                fail("Should not get here");
            }
        }
    }

    @Test
    public void testParseBigDecimal() throws Exception {
        assertEquals(new BigDecimal("1536.00"), ParseUtils.parseBigDecimal("1,536.00"));
    }

    @Test
    public void parseRMBPB_OFXTest() throws Exception {
        ParsedStatement parsedStatement = new OFXStatementParser().parse(this.getClass()
                .getResource("rmbpb_account.ofx").toURI());
        assertEquals("62012345678", parsedStatement.getAccountNumber());
        assertEquals(35, parsedStatement.getLines().size());
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        DateFormat txDateFormat = new SimpleDateFormat("dd MMM yyyy");
        int checked = 0;
        for (ParsedStatement.Line line : parsedStatement.getLines()) {
            if (dateFormat.parse("2022/11/03").getTime() == line.getPostedOn().getTime().getTime()) {
                assertEquals("TOTAL FAERIEGLEN 470720*5678  01 NOV", line.getDescription());
                assertEquals(txDateFormat.parse("01 NOV 2022"), line.getTransactionDate().getTime());
                assertEquals(new BigDecimal("-532.60"), line.getAmount());
                assertEquals(new BigDecimal("70216.72"), line.getBalance());
                checked++;
            }
        }
        assertEquals(1, checked);
    }

    @Test
    public void parseOFX2Test() throws Exception {
        ParsedStatement parsedStatement = new OFX2StatementParser().parse(this.getClass()
                .getResource("CapitecBankTransactionHistory_OFX2.ofx").toURI());
        assertEquals("1234567890", parsedStatement.getAccountNumber());
        assertEquals(36, parsedStatement.getLines().size());
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        int checked = 0;
        for (ParsedStatement.Line line : parsedStatement.getLines()) {
            if (dateFormat.parse("20230401").getTime() == line.getPostedOn().getTime().getTime()) {
                assertEquals("Superspar Johannesburg (Card 1234)", line.getDescription());
                assertEquals(dateFormat.parse("20230329"), line.getTransactionDate().getTime());
                assertEquals(new BigDecimal("-112.88"), line.getAmount());
                assertEquals(new BigDecimal("13316.75"), line.getBalance());
                checked++;
            }
        }
        assertEquals(1, checked);
    }

    @Test
    public void parseOFX1Test() throws Exception {
        ParsedStatement parsedStatement = new OFXStatementParser().parse(this.getClass()
                .getResource("CapitecBankTransactionHistory_OFX1.ofx").toURI());
        assertEquals("1234567890", parsedStatement.getAccountNumber());
        assertEquals(36, parsedStatement.getLines().size());
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        int checked = 0;
        for (ParsedStatement.Line line : parsedStatement.getLines()) {
            if (dateFormat.parse("20230401").getTime() == line.getPostedOn().getTime().getTime()) {
                assertEquals("Superspar Johannesburg (Card 1234)", line.getDescription());
                assertEquals(new BigDecimal("-112.88"), line.getAmount());
                assertEquals(new BigDecimal("13316.75"), line.getBalance());
                checked++;
            }
        }
        assertEquals(1, checked);
    }

    @Test
    public void compareStatementsTest() throws Exception {
        ParsedStatement parsedOFX1Statement = new OFXStatementParser().parse(this.getClass()
                .getResource("CapitecBankTransactionHistory_OFX1.ofx").toURI());
        ParsedStatement parsedOFX2Statement = new OFX2StatementParser().parse(this.getClass()
                .getResource("CapitecBankTransactionHistory_OFX2.ofx").toURI());
        List<String> ofx1Descriptions = parsedOFX1Statement.getLines().stream()
                .map(line -> line.getDescription()).toList();
        assertEquals(36, ofx1Descriptions.size());
        List<String> ofx2Descriptions = parsedOFX2Statement.getLines().stream()
                .map(line -> line.getDescription()).toList();
        assertEquals(36, ofx2Descriptions.size());
        ofx2Descriptions.stream().filter(line -> !ofx1Descriptions.contains(line)).forEach(System.out::println);
        assertTrue(ofx1Descriptions.containsAll(ofx2Descriptions));
    }

    @Test
    public void detectDuplicatesTest() throws Exception {
        String linea1 = "Netflix.com, Netherlands";
        String linea2 = "159.00 ZAR (@ rate R1.00), Online transaction";
        String lineb1 = "Netflix com Netherlands";
        String lineb2 = "159 00 ZAR  rate R1 00  Online transaction";
        linea2 = strip(linea2);
        linea2 = collapseSpaces(linea2);
        lineb2 = collapseSpaces(lineb2);
        System.out.println(linea2);
        System.out.println(lineb2);
        assertEquals(linea2, lineb2);
        linea1 = strip(linea1);
        linea2 = collapseSpaces(linea1);
        lineb2 = collapseSpaces(lineb1);
        System.out.println(linea2);
        System.out.println(lineb2);
        assertEquals(linea2, lineb2);
    }

    private String strip(String fromThis) {
        char[] chars = new char[fromThis.length()];
        fromThis.getChars(0, fromThis.length(), chars, 0);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            switch (chars[i]) {
                case ' ':
                    if (i < chars.length - 1 && chars[i + 1] == ' ') {
                        builder.append(' ');
                        i++;
                    } else builder.append(' ');
                    break;
                case '.': builder.append(' ');
                    break;
                case '(':
                case ')':
                case '@':
                case ',':
                    break;
                default:
                    builder.append(chars[i]);
            }
        }
        return builder.toString();
    }

    private String collapseSpaces(String fromThis) {
        char[] chars = new char[fromThis.length()];
        fromThis.getChars(0, fromThis.length(), chars, 0);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == ' ') {
                if (i < chars.length - 1 && chars[i + 1] == ' ') {
                    builder.append(' ');
                    i++;
                } else builder.append(' ');
            } else builder.append(chars[i]);
        }
        return builder.toString();
    }

}
