package hoogenbj.countary.util;

import hoogenbj.countary.model.TransactionHolder;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;

public class DateOrderSorter implements StatementSorter {
    @Override
    public SortedList<TransactionHolder> sort(ObservableList<TransactionHolder> transactions) {
        return new SortedList<>(transactions, (left, right) ->
                right.pdate().compareTo(left.pdate()));
    }
}
