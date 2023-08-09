package hoogenbj.countary.model;

import hoogenbj.countary.app.BigDecimalProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;

public class AccountBalanceHolder {
    private StringProperty accountNameProperty;
    private BigDecimalProperty amountProperty;

    private Account account;

    public AccountBalanceHolder() {
    }

    public AccountBalanceHolder(Account account, BigDecimal amount) {
        this.account = account;
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
