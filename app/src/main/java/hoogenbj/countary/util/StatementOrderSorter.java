package hoogenbj.countary.util;

import hoogenbj.countary.model.TransactionHolder;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;

public class StatementOrderSorter implements StatementSorter {
    @Override
    public SortedList<TransactionHolder> sort(ObservableList<TransactionHolder> transactions) {
        return new SortedList<>(transactions,
                (left, right) -> right.getId().compareTo(left.getId()));

    }
}
