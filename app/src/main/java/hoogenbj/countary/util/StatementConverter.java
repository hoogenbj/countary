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

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatementConverter {

    private static String[] posPurchases = new String[]{
            "Zapper",
            "SANRAL",
            "PIERRE VAN RYNEVELD",
            "Tops Ryneveld",
            "CLOUD",
            "BAGWORLD",
            "EXCLUSIVE BOOKS",
            "BWH",
            "SKECHERS",
            "WOOLWORTHS",
            "SuperSpar",
            "MILADYS",
            "DIS-CHEM",
            "WEFIX",
            "CAPPUCCINO",
            "PRINTAWAYS",
            "SEATTLE",
            "TAKEALOT",
            "Pierre van Ryneveld",
            "TOY KINGDOM",
            "INCRED CONNECT",
            "WORKWEAR DEPOT",
            "OSMANS",
            "Disney Plus",
            "ACSA JIA",
            "UBER TRIP",
            "MUGG AND BEAN",
            "Starbucks",
            "UBERZA",
            "Yoco",
            "CMH Car Hire TA Fir",
            "Snapscan Vermaak an",
            "SUPERSPAR RYNEVELD",
            "INTERPARK MENLYN",
            "WWW.SANRAL.CO.ZA",
            "CHECKERS",
            "Tops Seadoone",
            "ATKV NATALIA",
            "SEBAGO",
            "CAPE UNION MART",
            "STER KINEKO",
            "GAME",
            "MOZAMBIK",
            "CAFE VENTI",
            "ZAPPER ECOMM",
            "VOLPES",
            "SHOWMAX",
            "FlySafair",
            "NETFLIX",
            "GLODINA TOWELLING",
            "LINDT AND SPRUNGLI",
            "Gateway",
            "Makro",
            "DIE PADSTAL",
            "WIMPY RESTAURANT",
            "PNP CRP",
            "Dischem",
            "Netflix",
            "AFRIHOST",
            "EZ Shuttle Pty Ltd",
            "Daily Maver",
            "Afrihost",
    };

    public static void main(String... args) throws IOException {
        ParsedStatement parsedStatement = new RMBPB_CSVStatementParser().parse(new File("/Users/johan/private/bankstate/Sep2022/62073470446.csv").toURI());
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        DateFormat txDateFormat = new SimpleDateFormat("dd MMM");
        List<ParsedStatement.Line> lines = parsedStatement.getLines();
        lines.sort(Comparator.comparing(ParsedStatement.Line::getPostedOn));
        Pattern regex1 = Pattern.compile(".*(\\d{6}\\*\\d{4}\\s{2}\\d{2}\\s[A-Z]{3})$");
        Pattern regex2 = Pattern.compile("\\D*(\\d+)$");
        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        decimalFormatSymbols.setDecimalSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("#########0.00", decimalFormatSymbols);
        int count = 1;
        for (ParsedStatement.Line line : lines) {
            Matcher matcher1 = regex1.matcher(line.getDescription());
            String description = line.getDescription();
            String reference = "";
            if (matcher1.matches()) {
                description = description.substring(0, description.length() - 19);
                reference = matcher1.group(1);
            }
            if (reference.isBlank()) {
                Matcher matcher2 = regex2.matcher(line.getDescription());
                if (matcher2.matches()) {
                    reference = matcher2.group(1);
                }
            }
            description = annotate(description, reference);
        }
    }

    private static String annotate(String description, String reference) {
        if (Arrays.stream(posPurchases).anyMatch(description::contains)) {
            return String.format("POS Purchase %s", description);
        } else if (description.startsWith("SATRIX")
                || description.startsWith("UIF")
                || description.startsWith("NETSTAR")
                || description.startsWith("LIBERTY")
                || description.startsWith("DISC PREM")) {
            return String.format("Magtape Debit %s", description);
        } else if (description.startsWith("ABSA BANK Span Digital")) {
            return String.format("Magtape Credit %s", description);
        } else if (description.startsWith("Total")
                || description.startsWith("TOTAL")
                || description.startsWith("ENGEN")) {
            return String.format("Fuel Purchase %s", description);
        } else if (description.startsWith("OUTSURANCE")) {
            return String.format("Internal Debit Order %s", description);
        } else if (description.startsWith("DR HENNING")
                || description.startsWith("ATTIE BATTERY")
                || description.startsWith("RADIOLO-")
                || description.startsWith("JOHN")
                || description.startsWith("PSC")
                || description.startsWith("MARTHA")
                || description.startsWith("NATALIA")) {
            return String.format("Internet Pmt To %s", description);
        } else if (description.startsWith("SABCSAUKTV")) {
            return String.format("Magtape Debit %s", description);
        } else if (description.startsWith("INTERNET TRF FROM")) {
            return String.format("Internet Trf From %s", description);
        } else if (description.startsWith("SCHEDU AIRTIME TOPUP")) {
            return String.format("Schedu Airtime Topup To Airtime %s", reference);
        } else if (description.startsWith("TELEFOONREKENING")
                || description.startsWith("CITY OF TSHWANE")
                || description.startsWith("RONELL")
                || description.startsWith("S&G")
                || description.startsWith("KERK")
                || description.startsWith("JOHAN SAKGELD")
                || description.startsWith("LS3CPF")
                || description.startsWith("V/D BERG")
                || description.startsWith("PILATES")
                || description.startsWith("PA")) {
            return String.format("Scheduled Payment To %s", description);
        } else {
            return description;
        }
    }
}
