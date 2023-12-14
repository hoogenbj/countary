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

import javafx.scene.paint.Color;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.util.Locale;

public class ParseUtils {

    public static String DECIMAL_FORMAT_SYMBOLS = "#,###,###,##0.00";
    public static String SIMPLE_DECIMAL_FORMAT_SYMBOLS = "#########0.00";
    public static final String DATE_FORMAT_SYMBOLS = "yyyy/MM/dd";

    public static String toRGBCode(Color color) {
        double red = color.getRed() * 255;
        double green = color.getGreen() * 255;
        double blue = color.getBlue() * 255;
        return String.format("#%02X%02X%02X",
                Double.valueOf(red).intValue(),
                Double.valueOf(green).intValue(),
                Double.valueOf(blue).intValue());
    }

    public static String stripQuotesAndWhiteSpace(String field) {
        String[] parts = field.split("'|\"|\\s");
        return String.join("", parts);
    }

    public static BigDecimal parseBigDecimal(String val) {
        DecimalFormat format = new DecimalFormat(DECIMAL_FORMAT_SYMBOLS, DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        format.setParseBigDecimal(true);
        ParsePosition pos = new ParsePosition(0);
        return (BigDecimal) format.parse(val, pos);
    }

    public static String formatBigDecimal(BigDecimal value) {
        DecimalFormat format = new DecimalFormat(DECIMAL_FORMAT_SYMBOLS, DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return format.format(value);
    }

    public static String formatBigDecimalSimple(BigDecimal value) {
        DecimalFormat format = new DecimalFormat(SIMPLE_DECIMAL_FORMAT_SYMBOLS, DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        return format.format(value);
    }
}
