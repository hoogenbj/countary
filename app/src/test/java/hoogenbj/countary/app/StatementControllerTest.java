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

package hoogenbj.countary.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import hoogenbj.countary.di.TestGuiceModule;
import hoogenbj.countary.model.*;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import org.jmock.Expectations;
import org.jmock.junit5.JUnit5Mockery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StatementControllerTest {

    @RegisterExtension
    JUnit5Mockery context = new JUnit5Mockery();
    private static Injector injector;

    @BeforeAll
    static void beforeAll() {
        // needed to get rid of Toolkit error
        new JFXPanel();
    }

    @BeforeEach
    void before() {
        injector = Guice.createInjector(new TestGuiceModule(context));
    }

    @Test
    public void testNoDatabaseUrlNoDatabase() throws Exception {
        Settings settings = injector.getInstance(Settings.class);
        UserInterface userInterface = injector.getInstance(UserInterface.class);
        DataModel dataModel = injector.getInstance(DataModel.class);
        context.checking(new Expectations() {
            {
                oneOf(userInterface).showWarning(with(any(String.class)));
                allowing(userInterface).chooseDB();
                will(returnValue("Nothing"));
                allowing(dataModel).tableExists("account");
            }
        });
        TransactionController controller = new TransactionController(settings, userInterface, dataModel,
                (a, b) -> {
                }, (c) -> {
        }, (d) -> {
        });
        try {
            controller.createNode();
            Assertions.assertNotNull(controller.accounts);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testNoDatabaseUrl() throws Exception {
        Settings settings = injector.getInstance(Settings.class);
        UserInterface userInterface = injector.getInstance(UserInterface.class);
        DataModel dataModel = injector.getInstance(DataModel.class);
        Account account = new Account(1L, "name", "number", "123456", "bank", "tagColor");
        List<KeyValue> accounts = new ArrayList<>();
        accounts.add(Account.toKeyValue(account));
        String dbUrl = "jdbc:sqlite:/path/to/database";
        KeyValue currentAccount = new KeyValue("account", "1");
        List<Transaction> transactionList = new ArrayList<>();
        transactionList.add(new Transaction(1L, account, "2021/12/13", "2021/12/13", "-43.90", "3781.12",
                "UIF       123456789123456", 1l, false));
        transactionList.add(new Transaction(1L, account, "2021/12/13", "2021/12/13", "-2000.00", "5781.12",
                "UIF       123456789123456", 2l, false));
        Budget budget = new Budget(1L, null, "expenses", Kind.Monthly, false);

        context.checking(new Expectations() {
            {
                allowing(settings).getDatabaseUrl();
                will(returnValue(null));
                allowing(userInterface).openDatabaseFile();
                String result = "/path/to/database/file";
                will(returnValue(result));
                allowing(settings).setDatabaseUrl("jdbc:sqlite:" + result);
                allowing(settings).setDatabasePath(result);
                allowing(dataModel).getAccountsList();
                will(returnValue(accounts));
                allowing(dataModel).tableExists(with("account"));
                will(returnValue(true));
                allowing(dataModel).getAccounts();
                will(returnValue(List.of(account)));
                allowing(dataModel).getBudgets();
                will(returnValue(List.of(budget)));
                allowing(dataModel).getTransactions(with(account), with(false));
                will(returnValue(transactionList));
                allowing(dataModel).getAccount(with(1l));
                will(returnValue(account));
                allowing(settings).getCurrentAccount();
                will(returnValue(null));
                allowing(userInterface).chooseDB();
                will(returnValue("Nothing"));
                allowing(userInterface).showWarning(with("Unfortunately it is not possible to continue without a database"));
            }
        });
        TransactionController controller = new TransactionController(settings, userInterface, dataModel,
                (a, b) -> {
                }, (c) -> {
        }, (d) -> {
        });
        try {
            controller.createNode();
            Assertions.assertNotNull(controller.accounts);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
