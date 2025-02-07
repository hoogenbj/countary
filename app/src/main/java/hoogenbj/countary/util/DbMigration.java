package hoogenbj.countary.util;

import hoogenbj.countary.app.UserInterface;
import hoogenbj.countary.model.DataModel;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class DbMigration {

    private final Consumer<String> stringConsumer;
    private final DataModel model;
    private final UserInterface userInterface;

    public DbMigration(DataModel model, UserInterface userInterface, Consumer<String> stringConsumer) {
        this.model = model;
        this.stringConsumer = stringConsumer;
        this.userInterface = userInterface;
    }

    public void migrate(int to) {
        if (to == 2)
            migrateTo2();
        else
            userInterface.showWarning(String.format("Migration to version %d not supported by this version of the software", to));
    }

    private void migrateTo2() {
        try {
            List<String> statements = IOUtils.readLines(Objects
                    .requireNonNull(DbUtils.class.getResourceAsStream("migrate_to2.sql")), (Charset) null);
            model.executeStatements(statements, false);
            stringConsumer.accept("Migration finished.");
            stringConsumer.accept("You can now restart the application.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
