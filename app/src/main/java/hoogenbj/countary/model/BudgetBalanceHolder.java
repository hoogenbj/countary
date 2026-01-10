package hoogenbj.countary.model;

import hoogenbj.countary.app.BigDecimalProperty;
import hoogenbj.countary.app.KindProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;

public class BudgetBalanceHolder {
    private StringProperty budgetNameProperty;
    private KindProperty kindProperty;
    private BigDecimalProperty balanceProperty;
    private Budget budget;

    public BudgetBalanceHolder() {
    }

    public BudgetBalanceHolder(Budget budget, BigDecimal balance) {
        this.budget = budget;
        setBudgetName(budget.name());
        setKind(budget.kind());
        setBalance(balance);
    }

    private void setBalance(BigDecimal balance) {
        balanceProperty().set(balance);
    }

    public Budget getBudget() {
        return budget;
    }

    public BigDecimalProperty balanceProperty() {
        if (balanceProperty == null) balanceProperty = new BigDecimalProperty(this, "balance");
        return balanceProperty;
    }

    public void setBudgetName(String name) {
        this.budgetNameProperty().set(name);
    }

    public void setKind(Kind kind) {
        this.kindProperty().set(kind);
    }

    public StringProperty budgetNameProperty() {
        if (budgetNameProperty == null) budgetNameProperty = new SimpleStringProperty(this, "budgetName");
        return budgetNameProperty;
    }

    public KindProperty kindProperty() {
        if (kindProperty == null) kindProperty = new KindProperty(this, "kind");
        return kindProperty;
    }

    public String getBudgetName() {
        return budgetNameProperty.get();
    }

    public Kind getKind() {
        return kindProperty.get();
    }

    public BigDecimal getBalance() {
        return balanceProperty().get();
    }
}
