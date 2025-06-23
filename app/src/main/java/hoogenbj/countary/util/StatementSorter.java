package hoogenbj.countary.util;

import hoogenbj.countary.model.TransactionHolder;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;

public interface StatementSorter {
    SortedList<TransactionHolder> sort(ObservableList<TransactionHolder> transactions);
}
