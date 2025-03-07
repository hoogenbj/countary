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

package hoogenbj.countary.util.ofx2;

import hoogenbj.countary.util.ParseUtils;
import hoogenbj.countary.util.ParsedStatement;
import hoogenbj.countary.util.StatementParseException;
import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {
    public static Header parseHeader(Node node, String path) {
        String trimmed = node.getNodeValue().trim();
        if (trimmed.isEmpty())
            throw new StatementParseException("Expected statement to contain a OFX processing instruction, but found nothing", path);
        String[] keyValues = trimmed.split("\\s+");
        if (keyValues.length == 0)
            throw new StatementParseException("Expected statement to contain a OFX processing instruction, but found nothing", path);
        Map<String, String> map = getPseudoAttributes(trimmed);
        if (!map.containsKey("OFXHEADER"))
            throw new StatementParseException("Expected OFX processing instruction to contain OFXHEADER=value, but found nothing", path);
        if (!map.containsKey("VERSION"))
            throw new StatementParseException("Expected OFX processing instruction to contain VERSION=value, but found nothing", path);
        if (!map.containsKey("SECURITY"))
            throw new StatementParseException("Expected OFX processing instruction to contain SECURITY=value, but found nothing", path);
        if (!map.containsKey("OLDFILEUID"))
            throw new StatementParseException("Expected OFX processing instruction to contain OLDFILEUID=value, but found nothing", path);
        if (!map.containsKey("NEWFILEUID"))
            throw new StatementParseException("Expected OFX processing instruction to contain NEWFILEUID=value, but found nothing", path);
        return new Header(Integer.parseInt(map.get("OFXHEADER")),
                Integer.parseInt(map.get("VERSION")),
                map.get("SECURITY"),
                map.get("OLDFILEUID"),
                map.get("NEWFILEUID"));
    }

    private static Map<String, String> getPseudoAttributes(String trimmed) {
        Map<String, String> map = new HashMap<>();
        Pattern pattern = Pattern.compile("([A-Z]+)=\\\"(\\d+|[A-Z]+)\\\"");
        Matcher matcher = pattern.matcher(trimmed);
        while (matcher.find()) {
            map.put(matcher.group(1), matcher.group(2));
        }
        return map;
    }

    public static boolean validate(Header header, Node body, String path) {
        if (header.ofxHeader() != 200)
            throw new StatementParseException(String.format("Expected OFXHEADER to have value 200 but found %d", header.ofxHeader()), path);
        if (header.version() < 200)
            throw new StatementParseException(String.format("Expected VERSION to have a value greater or equal to 200 but found %d", header.version()), path);
        if (!body.getNodeName().equals("OFX"))
            throw new StatementParseException(String.format("Expected statement to contain an OFX xml element as root but found %s", body.getLocalName()), path);
        return true;
    }

    public static ParsedStatement parseBody(Node body, String path) {
        NodeList nodes = body.getChildNodes();
        ParsedStatement statement = new ParsedStatement();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeName().equals("BANKMSGSRSV1")) {
                NodeList msgNodes = nodes.item(i).getChildNodes();
                for (int j=0; j<msgNodes.getLength(); j++) {
                    if (msgNodes.item(j).getNodeName().equals("STMTTRNRS")) {
                        parseStatement(msgNodes.item(j), statement, path);
                    }
                }
            }
        }
        return statement;
    }

    private static void parseStatement(Node item, ParsedStatement statement, String path) {
        BigDecimal balance = null;
        List<StatementTransaction> transactionList = null;
        for (int i=0; i<item.getChildNodes().getLength(); i++) {
            if (item.getChildNodes().item(i).getNodeName().equals("STMTRS")) {
                NodeList stmtrs = item.getChildNodes().item(i).getChildNodes();
                for (int j=0; j<stmtrs.getLength(); j++) {
                    if (stmtrs.item(j).getNodeName().equals("BANKACCTFROM")) {
                        if (!parseBankAccount(stmtrs.item(j), statement))
                            throw new StatementParseException("Could not find bank account number", path);
                    } else if (stmtrs.item(j).getNodeName().equals("BANKTRANLIST")) {
                        transactionList = parseTransactions(stmtrs.item(j), path);
                    } else if (stmtrs.item(j).getNodeName().equals("LEDGERBAL")) {
                        balance = parseBalance(stmtrs.item(j), statement);
                        if (balance == null)
                            throw new StatementParseException("Could not find balance", path);
                    }
                }
            }
        }
        if (transactionList == null || transactionList.isEmpty())
            throw new StatementParseException("No transactions found in statement", path);
        final BigDecimal[] runningBalance = {balance};
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        DateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMddhhmmss");
        List<ParsedStatement.Line> transactions = new ArrayList<>();
        AtomicInteger transactionCount = new AtomicInteger();
        transactionList.forEach(trn -> {
            transactionCount.getAndIncrement();
            ParsedStatement.Line transaction = new ParsedStatement.Line();
            Calendar postingDate = Calendar.getInstance();
            try {
                postingDate.setTime(dateFormat.parse(trn.dtposted().substring(0, 8)));
                transaction.setPostedOn(postingDate);
            } catch (ParseException e) {
                throw new RuntimeException(String.format("Unable to parse date %s in transaction " +
                                "%d of statement at %s ", trn.dtposted(), transactionCount.get(),
                        path), e);
            }
            Calendar transactionDate = Calendar.getInstance();
            try {
                transactionDate.setTime(dateTimeFormat.parse(trn.dtuser().substring(0, 14)));
                transaction.setTransactionDate(transactionDate);
            } catch (ParseException e) {
                throw new RuntimeException(String.format("Unable to parse date-time %s in transaction " +
                                "%d of statement at %s ", trn.dtposted(), transactionCount.get(),
                        path), e);
            }
            BigDecimal amount = ParseUtils.parseBigDecimal(trn.trnamt());
            transaction.setAmount(amount);
            transaction.setDescription(StringEscapeUtils.unescapeXml(trn.memo()));
            transactions.add(transaction);
        });
        transactions.stream().sorted(Comparator.comparing(ParsedStatement.Line::getTransactionDate).reversed())
                .forEach(transaction -> {
                    transaction.setBalance(runningBalance[0]);
                    runningBalance[0] = runningBalance[0].subtract(transaction.getAmount());
                });
        statement.setLines(transactions);
    }

    private static BigDecimal parseBalance(Node item, ParsedStatement statement) {
        BigDecimal balance = null;
        for (int i=0; i<item.getChildNodes().getLength(); i++) {
            if (item.getChildNodes().item(i).getNodeName().equals("BALAMT")) {
                Node balamt = item.getChildNodes().item(i);
                return ParseUtils.parseBigDecimal(balamt.getTextContent());
            }
        }
        return balance;
    }

    private static List<StatementTransaction> parseTransactions(Node item, String path) {
        List<StatementTransaction> transactionList = new ArrayList<>();
        int transactionCount = 0;
        for (int i=0; i<item.getChildNodes().getLength(); i++) {
            if (item.getChildNodes().item(i).getNodeName().equals("STMTTRN")) {
                transactionCount++;
                Map<String, String> fields = getTransactionFields(item.getChildNodes().item(i));
                if (validTransaction(fields)) {
                    transactionList.add(new StatementTransaction(fields.get("TRNTYPE"), fields.get("DTPOSTED"),
                            fields.get("DTUSER"), fields.get("TRNAMT"), fields.get("FITID"), fields.get("MEMO")));
                } else
                    throw new StatementParseException(String.format("Transaction number %d is invalid", transactionCount), path);
            }
        }
        return transactionList;
    }

    private static void pluck(String key, Node node, Map<String, String> fields) {
        if (node.getNodeName().equals(key))
            fields.put(key, node.getTextContent());
    }

    private static final Set<String> fieldNames = Set.of("TRNTYPE", "DTPOSTED", "DTUSER", "TRNAMT", "FITID", "MEMO");
    private static Map<String, String> getTransactionFields(Node item) {
        Map<String, String> fields = new HashMap<>();
        for (int i=0; i<item.getChildNodes().getLength(); i++) {
            Node node = item.getChildNodes().item(i);
            fieldNames.forEach(name -> {if (node.getNodeName().equals(name)) fields.put(name, node.getTextContent());});
        }
        return fields;
    }

    private static boolean validTransaction(Map<String, String> fields) {
        return fields.keySet().containsAll(fieldNames);
    }

    private static boolean parseBankAccount(Node item, ParsedStatement statement) {
        boolean found = false;
        for (int i=0; i<item.getChildNodes().getLength(); i++) {
            if (item.getChildNodes().item(i).getNodeName().equals("ACCTID")) {
                Node acctid = item.getChildNodes().item(i);
                statement.setAccountNumber(acctid.getTextContent());
                found = statement.getAccountNumber() != null && statement.getAccountNumber().length() > 0;
            }
        }
        return found;
    }
}
