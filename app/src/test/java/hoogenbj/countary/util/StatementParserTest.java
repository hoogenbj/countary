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

import static org.junit.jupiter.api.Assertions.*;

public class StatementParserTest {

    @Test
    public void parseRMBPBTest() throws Exception {
        ParsedStatement parsedStatement = new RMBPB_CSVStatementParser().parse(this.getClass().getResource("rmbpb_account.csv").toURI());
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
        ParsedStatement parsedStatement = new RMPB_OFXStatementParser().parse(this.getClass().getResource("rmbpb_account.ofx").toURI());
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
}
