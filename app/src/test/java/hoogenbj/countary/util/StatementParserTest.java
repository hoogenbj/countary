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

import static hoogenbj.countary.util.ParseUtils.stripQuotes;
import static org.junit.jupiter.api.Assertions.*;

public class StatementParserTest {

    @Test
    public void parseCapitecCsvTest() throws Exception {
        ParsedStatement parsedStatement = new Capitec_CSVStatementParser()
                .parse(this.getClass().getResource("CapitecBankTransactionHistory_123-123.csv").toURI());
        assertEquals("1234567890", parsedStatement.getAccountNumber());
        DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        Date expectedDate = GregorianCalendar.from(dateFormat.parse("18/04/2023", LocalDate::from).atStartOfDay(ZoneId.systemDefault())).getTime();
        int checked = 0;
        assertEquals(32, parsedStatement.getLines().size());
        for (ParsedStatement.Line line :
                parsedStatement.getLines()) {
            if (expectedDate.equals(line.getPostedOn().getTime())) {
                assertEquals("Total Johannesburg (Card 1234)", line.getDescription());
                assertEquals(GregorianCalendar.from(dateFormat.parse("14/04/2023", LocalDate::from).atStartOfDay(ZoneId.systemDefault())).getTime(), line.getTransactionDate().getTime());
                assertEquals(new BigDecimal("-65.90"), line.getAmount());
                assertEquals(new BigDecimal("17144.59"), line.getBalance());
                checked++;
            }
        }
        assertEquals(1, checked);
    }

    @Test
    public void testHash() throws Exception {
        String[] fields = List.of("3", "1234567890", "08/04/2023", "05/04/2023", "Cx Roodepoort (Card 1234)",
                "Cx Roodepoort         Roodepoort   ZA", "Food", "Groceries", "63.97", "", "20151.53").toArray(new String[0]);
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
        ParsedStatement parsedStatement = new BankZeroStatementParser().parse(this.getClass().getResource("bankzero_account.csv").toURI());
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        for (ParsedStatement.Line line : parsedStatement.getLines()) {
            if (dateFormat.parse("04/12/2021").equals(line.getPostedOn().getTime())) {
                assertEquals("Pay in,Capitec bank limited,CAPITEC  JANE", line.getDescription());
                assertEquals(new BigDecimal("600.00"), line.getAmount());
                assertEquals(new BigDecimal("1536.00"), line.getBalance());
            } else if (dateFormat.parse("27/12/2021").equals(line.getPostedOn().getTime())) {
                assertEquals("Card purchase,Exclusive Books Sandton,Sandton Dip Transaction", line.getDescription());
                assertEquals(new BigDecimal("-206.00"), line.getAmount());
                assertEquals(ParseUtils.parseBigDecimal("1,330.00"), line.getBalance());
            } else if (dateFormat.parse("31/12/2021").equals(line.getPostedOn().getTime())) {
                assertEquals("Card purchase,Loot Online,Johannesburg Online Transaction", line.getDescription());
                assertEquals(ParseUtils.parseBigDecimal("-1,189.00"), line.getAmount());
                assertEquals(new BigDecimal("141.00"), line.getBalance());
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
        ParsedStatement parsedStatement = new OFXStatementParser().parse(this.getClass().getResource("rmbpb_account.ofx").toURI());
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
        ParsedStatement parsedStatement = new OFX2StatementParser().parse(this.getClass().getResource("CapitecBankTransactionHistory_OFX2.ofx").toURI());
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
        ParsedStatement parsedStatement = new OFXStatementParser().parse(this.getClass().getResource("CapitecBankTransactionHistory_OFX1.ofx").toURI());
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
        ParsedStatement parsedOFX1Statement = new OFXStatementParser().parse(this.getClass().getResource("CapitecBankTransactionHistory_OFX1.ofx").toURI());
        ParsedStatement parsedOFX2Statement = new OFX2StatementParser().parse(this.getClass().getResource("CapitecBankTransactionHistory_OFX2.ofx").toURI());
        List<String> ofx1Descriptions = parsedOFX1Statement.getLines().stream().map(line -> line.getDescription()).toList();
        assertEquals(36, ofx1Descriptions.size());
        List<String> ofx2Descriptions = parsedOFX2Statement.getLines().stream().map(line -> line.getDescription()).toList();
        assertEquals(36, ofx2Descriptions.size());
        ofx2Descriptions.stream().filter(line -> !ofx1Descriptions.contains(line)).forEach(System.out::println);
        assertTrue(ofx1Descriptions.containsAll(ofx2Descriptions));
    }
}
