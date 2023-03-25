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

import hoogenbj.countary.util.ofx.*;
import org.apache.commons.text.StringEscapeUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RMPB_OFXStatementParser implements StatementParser {
    // Probably specific to RMBPB
    private static final Pattern txDatePattern = Pattern.compile("(\\d{2} (JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))$");
    public static final String BANKTRANLIST = "BANKTRANLIST";
    public static final String BANKACCTFROM = "BANKACCTFROM";
    public static final String LEDGERBAL = "LEDGERBAL";
    private static final Set<String> ofInterest = Set.of(BANKACCTFROM, BANKTRANLIST, LEDGERBAL);
    public static final String BALAMT = "BALAMT";
    public static final String ACCTID = "ACCTID";

    public static class OFXTransaction {
        public String description;
        public Calendar postingDate;
        public BigDecimal amount;
        public BigDecimal balance;

        public boolean isValid() {
            return description != null && postingDate != null && amount != null && balance != null;
        }

        @Override
        public String toString() {
            return "OFXTransaction{" +
                    "description='" + description + '\'' +
                    ", postingDate=" + postingDate +
                    ", amount=" + amount +
                    ", balance=" + balance +
                    '}';
        }
    }

    @Override
    public ParsedStatement parse(URI uri) throws IOException, StatementParseException {
        List<String> lines = Files.readAllLines(Path.of(uri));
        ListIterator<String> iterator = lines.listIterator();
        // Maybe we'll need headers in future
        List<Header> headers = new ArrayList<>();
        Matcher header;
        do {
            String line = iterator.next();
            header = Tags.HEADER_REGEX.matcher(line);
            if (header.matches())
                headers.add(new Header(header.group(1), header.group(2)));
        } while (header.matches());
        // There are some complex tags (i.e. tags containing tags) we are interested in
        Map<String, ComplexTag> tags = new HashMap<>();
        Tag nextTag = new ComplexTag(null, "ROOT");
        int count = 0;
        try {
            while (iterator.hasNext() && nextTag != null) {
                String line = iterator.next();
                if (!line.isBlank()) {
                    nextTag = nextTag.processLine(line);
                    if (nextTag != null && ofInterest.contains(nextTag.getName()))
                        tags.put(nextTag.getName(), (ComplexTag) nextTag);
                }
                count++;
            }
        } catch (Exception e) {
            throw new StatementParseException(uri.getPath(), count + 1, e);
        }
        if (!tags.containsKey(BANKACCTFROM))
            throw new RuntimeException(String.format("Statement at %s has no %s tag", uri.getPath(), BANKACCTFROM));
        if (!tags.containsKey(BANKTRANLIST))
            throw new RuntimeException(String.format("Statement at %s has no %s tag", uri.getPath(), BANKTRANLIST));
        if (!tags.containsKey(LEDGERBAL))
            throw new RuntimeException(String.format("Statement at %s has no %s tag", uri.getPath(), LEDGERBAL));
        ParsedStatement parsedStatement = new ParsedStatement();
        List<ParsedStatement.Line> statementLines = new ArrayList<>();
        parsedStatement.setLines(statementLines);
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        DateFormat txDateFormat = new SimpleDateFormat("dd MMM yyyy");
        tags.get(BANKACCTFROM).getChildren().forEach(tag -> {
            if (tag instanceof SimpleTag simpleTag) {
                if (tag.getName().equals(ACCTID)) {
                    parsedStatement.setAccountNumber(simpleTag.getContent());
                }
            }
        });
        if (parsedStatement.getAccountNumber() == null || parsedStatement.getAccountNumber().isEmpty())
            throw new RuntimeException(String.format("Expected to find account number in tag %s of " +
                    "statement at %s but found nothing", ACCTID, uri.getPath()));
        final BigDecimal[] runningBalance = {null};
        tags.get(LEDGERBAL).getChildren().forEach(tag -> {
            if (tag instanceof SimpleTag simpleTag) {
                if (tag.getName().equals(BALAMT)) {
                    runningBalance[0] = ParseUtils.parseBigDecimal(simpleTag.getContent());
                }
            }
        });
        if (runningBalance[0] == null)
            throw new RuntimeException(String.format("Unable to get %s from %s tag in statement at %s", BALAMT,
                    LEDGERBAL, uri.getPath()));
        final int[] transactionCount = {0};
        tags.get(BANKTRANLIST).getChildren().forEach(t -> {
            transactionCount[0] = transactionCount[0] + 1;
            if (t instanceof ComplexTag complexTag) {
                OFXTransaction transaction = new OFXTransaction();
                complexTag.getChildren().forEach(c -> {
                    if (c instanceof SimpleTag simpleTag) {
                        switch (c.getName()) {
                            case "DTPOSTED" -> {
                                Calendar postingDate = Calendar.getInstance();
                                try {
                                    postingDate.setTime(dateFormat.parse(simpleTag.getContent()));
                                    transaction.postingDate = postingDate;
                                } catch (ParseException e) {
                                    throw new RuntimeException(String.format("Unable to parse date %s in transaction " +
                                                    "%d of statement at %s ", simpleTag.getContent(), transactionCount[0],
                                            uri.getPath()), e);
                                }
                            }
                            case "TRNAMT" -> {
                                BigDecimal amount = ParseUtils.parseBigDecimal(simpleTag.getContent());
                                transaction.amount = amount;
                                transaction.balance = runningBalance[0];
                                runningBalance[0] = runningBalance[0].add(amount);
                            }
                            case "MEMO" ->
                                    transaction.description = StringEscapeUtils.unescapeXml(simpleTag.getContent());
                        }
                    }
                });
                if (!transaction.isValid())
                    throw new RuntimeException(String.format("Expected to find MEMO, TRNAMT, DTPOSTED in " +
                                    "transaction number %d of statement at %s, but instead found %s", transactionCount[0],
                            uri.getPath(), transaction));
                ParsedStatement.Line l = new ParsedStatement.Line();
                l.setDescription(transaction.description);
                l.setAmount(transaction.amount);
                l.setBalance(transaction.balance);
                l.setPostedOn(transaction.postingDate);
                Matcher matcher = txDatePattern.matcher(transaction.description);
                if (matcher.find()) {
                    String dateField = matcher.group(1);
                    String yearPortion = String.valueOf(transaction.postingDate.get(Calendar.YEAR));
                    Calendar txDate = Calendar.getInstance();
                    String completeDate = dateField + " " + yearPortion;
                    try {
                        txDate.setTime(txDateFormat.parse(completeDate));
                    } catch (ParseException e) {
                        throw new RuntimeException(String.format("Unable to parse date from %s in transaction %d " +
                                "of statement at %s", completeDate, transactionCount[0], uri.getPath()), e);
                    }
                    if (transaction.postingDate.get(Calendar.MONTH) == Calendar.JANUARY &&
                            txDate.get(Calendar.MONTH) == Calendar.DECEMBER) {
                        // transaction date is in the previous year
                        txDate.set(Calendar.YEAR, transaction.postingDate.get(Calendar.YEAR) - 1);
                    }
                    l.setTransactionDate(txDate);
                }
                statementLines.add(l);
            }
        });
        return parsedStatement;
    }
}
