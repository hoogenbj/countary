package hoogenbj.countary.util.ofx2;

import hoogenbj.countary.util.ParsedStatement;

import java.util.Objects;

public class TransactionLine extends ParsedStatement.Line {
    private String fitid;

    public String getFitid() {
        return fitid;
    }

    public void setFitid(String fitid) {
        this.fitid = fitid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fitid);
    }
}
