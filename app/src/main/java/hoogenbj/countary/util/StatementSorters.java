package hoogenbj.countary.util;

public enum StatementSorters {
    Natural("Statement Order", new StatementOrderSorter()),
    Date("Date Order", new DateOrderSorter());

    private final String description;
    private final StatementSorter sorter;

    StatementSorters(String description, StatementSorter sorter) {
        this.description = description;
        this.sorter = sorter;
    }

    public String getDescription() {
        return description;
    }

    public StatementSorter getSorter() {
        return sorter;
    }
}
