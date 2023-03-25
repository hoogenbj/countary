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

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class ParsedStatement {
    private String accountHolder;
    private String accountNumber;
    private List<Line> lines;

    public ParsedStatement() {
    }

    public static class Line {
        private Calendar postedOn;
        private Calendar transactionDate;
        private BigDecimal amount;
        private BigDecimal balance;
        private String description;

        public Line(Calendar postedOn, Calendar transactionDate, BigDecimal amount, BigDecimal balance, String description) {
            this.postedOn = postedOn;
            this.transactionDate = transactionDate;
            this.amount = amount;
            this.balance = balance;
            this.description = description;
        }

        public Line() {
        }

        public Calendar getPostedOn() {
            return postedOn;
        }

        public Calendar getTransactionDate() {
            return transactionDate;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setPostedOn(Calendar postedOn) {
            this.postedOn = postedOn;
        }

        public void setTransactionDate(Calendar transactionDate) {
            this.transactionDate = transactionDate;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return "Line{" +
                    "postedOn=" + format.format(postedOn.getTime()) +
                    ", transactionDate=" + (transactionDate != null ? format.format(transactionDate.getTime()) : null) +
                    ", amount=" + amount +
                    ", balance=" + balance +
                    ", description='" + description + '\'' +
                    '}';
        }

        @Override
        public int hashCode() {
            return Objects.hash(postedOn, description, amount, transactionDate, balance);
        }
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountHolder(String accountHolder) {
        this.accountHolder = accountHolder;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public void setLines(List<Line> lines) {
        this.lines = lines;
    }

    public List<Line> getLines() {
        return lines;
    }
}
