/*
 * Copyright (c) 2023. Johan Hoogenboezem
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

import hoogenbj.countary.model.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import java.io.IOException;

public class SummaryController extends StackPane implements ControllerHelpers {
    @FXML
    private Text debits;
    @FXML
    private Text credits;
    @FXML
    private Text transactionBalance;
    @FXML
    private Text funded;
    @FXML
    private Text unfunded;
    @FXML
    private Text fundingBalance;
    private SummaryHolder holder = new SummaryHolder();
    private DataModel dataModel;
    private SummaryModel model;

    public SummaryController() {
    }

    public static SummaryController getInstance(DataModel dataModel) {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(DisplayTagsControl.class.getResource("Summary.fxml"));
        SummaryController root = new SummaryController();
        root.dataModel = dataModel;
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(root);
        try {
            return fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void initialize() {
        debits.textProperty().bind(holder.transactionDebitsProperty());
        credits.textProperty().bind(holder.transactionCreditsProperty());
        funded.textProperty().bind(holder.budgetFundedProperty());
        unfunded.textProperty().bind(holder.budgetUnfundedProperty());
        fundingBalance.textProperty().bind(holder.budgetBalanceProperty());
        transactionBalance.textProperty().bind(holder.transactionBalanceProperty());
        model = new SummaryModel(dataModel, holder);
    }

    public void update(Account account, Budget budget) {
        model.update(account, budget);
    }

    public void update(Account account) {
        model.update(account);
    }
}
