package hoogenbj.countary.model;

import hoogenbj.countary.app.BigDecimalProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;

public class AccountBalanceHolder {
    private StringProperty accountNameProperty;
    private BigDecimalProperty amountProperty;

    private BigDecimal originalAmount;

    private Account account;

    public AccountBalanceHolder() {
    }

    public AccountBalanceHolder(Account account, BigDecimal amount) {
        this.account = account;
        this.originalAmount = amount;
        setAccountName(account.name());
        setAmount(amount);
        amountProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue) && newValue.signum() == oldValue.signum() &&
                    newValue.abs().compareTo(oldValue.abs()) < 1) {

            }
        });
    }

    public Account getAccount() {
        return account;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setAmount(BigDecimal amount) {
        this.amountProperty().set(amount);
    }

    public BigDecimalProperty amountProperty() {
        if (amountProperty == null) amountProperty = new BigDecimalProperty(this, "amount");
        return amountProperty;
    }

    public void setAccountName(String name) {
        this.accountNameProperty().set(name);
    }

    public StringProperty accountNameProperty() {
        if (accountNameProperty == null) accountNameProperty = new SimpleStringProperty(this, "accountName");
        return accountNameProperty;
    }

    public String getAccountName() {
        return accountNameProperty().get();
    }

    public BigDecimal getAmount() {
        return amountProperty().get();
    }
}
