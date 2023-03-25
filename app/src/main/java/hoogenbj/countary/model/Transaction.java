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
package hoogenbj.countary.model;

import hoogenbj.countary.util.ParseUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Objects;

/**
 * @param id          - The primary key
 * @param account     - The relevant account
 * @param postingDate - The date this transaction was posted
 * @param txdate      - The actual date of this transaction - if available. Otherwise null
 * @param amount
 * @param balance
 * @param description
 * @param hash        - For detecting duplicate transactions
 * @param allocated   - True if transaction is fully allocated
 * @param manual      - True if the transaction was captured manually
 * @param canDelete   - True if the transaction has not been allocated - either partially or completely.
 */
public record Transaction(Long id, Account account, Date postingDate, Date txdate, BigDecimal amount,
                          BigDecimal balance,
                          String description, Long hash, Boolean allocated, Boolean manual, Boolean canDelete) {
    private static final SimpleDateFormat format = new SimpleDateFormat(ParseUtils.DATE_FORMAT_SYMBOLS);

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Transaction(Long id, Account account, String postingDate, String txdate, String amount, String balance,
                       String description, Long hash, Boolean allocated) throws ParseException {
        this(id, account, format.parse(postingDate), (txdate != null ? format.parse(txdate) : null),
                new BigDecimal(amount), new BigDecimal(balance), description, hash, allocated, false, !allocated);
    }

    public Transaction addAllocated(boolean allocated) {
        return new Transaction(id, account, postingDate, txdate, amount, balance, description, hash, allocated, manual, !allocated);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", account=" + account +
                ", postingDate=" + postingDate +
                ", txdate=" + txdate +
                ", amount=" + amount +
                ", balance=" + balance +
                ", description='" + description + '\'' +
                ", hash=" + hash +
                ", allocated=" + allocated +
                ", manual=" + manual +
                ", canDelete=" + canDelete +
                '}';
    }
}
