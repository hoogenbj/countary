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

import hoogenbj.countary.util.ParseUtils;
import hoogenbj.countary.util.SVGUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Window;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

public class CalculateDlgController extends Dialog<Boolean> {

    @FXML
    private VBox calculationsContainer;
    @FXML
    private ButtonType okButtonType;
    private List<BigDecimal> transactions;
    private List<BigDecimal> planned;
    private List<BigDecimal> actual;

    private Button okButton;

    public static CalculateDlgController getInstance(Window owner, List<BigDecimal> planned, List<BigDecimal> actual) {
        CalculateDlgController controller = getInstance(owner);
        controller.planned = planned;
        controller.actual = actual;
        controller.initControls();
        return controller;
    }

    private static CalculateDlgController getInstance(Window owner) {
        FXMLLoader loader = CountaryApp.injector.getInstance(FXMLLoader.class);
        loader.setLocation(CalculateDlgController.class.getResource("CalculateDlg.fxml"));
        CalculateDlgController controller;
        try {
            DialogPane dlgPane = loader.load();
            controller = loader.getController();
            controller.setDialogPane(dlgPane);
            controller.initOwner(owner);
            controller.setTitle("Calculations");
            controller.okButton = (Button) dlgPane.lookupButton(controller.okButtonType);
            controller.okButton.setDefaultButton(true);
        } catch (IOException e) {
            throw new RuntimeException("Unable to launch " + ChangeCategoryDlgController.class.getSimpleName(), e);
        }
        return controller;
    }

    public static CalculateDlgController getInstance(Window owner, List<BigDecimal> transactions) {
        CalculateDlgController controller = getInstance(owner);
        controller.transactions = transactions;
        controller.initControls();
        return controller;
    }

    private void initControls() {
        if (transactions == null) {
            calculatePlannedAndActual();
        } else {
            calculateTransactions();
        }
    }

    private void calculateTransactions() {
        BigDecimal sum = transactions.stream().filter(CalculateDlgController::isValid).reduce(BigDecimal.ZERO, BigDecimal::add);
        addCalculation("Sum", sum);
        BigDecimal monthlyPayment = sum.divide(new BigDecimal(12), RoundingMode.DOWN);
        addCalculation("Sum / 12", monthlyPayment);
        BigDecimal annualPayment = sum.multiply(new BigDecimal(12));
        addCalculation("Sum x 12", annualPayment);
        BigDecimal average = sum.divide(BigDecimal.valueOf(transactions.size()), RoundingMode.DOWN);
        addCalculation("Average", average);
    }

    private void addCalculation(String description, BigDecimal result) {
        HBox line = makeLine();
        Label label = makeLabel(description);
        TextField field = makeField(result);
        Button copy = makeButton(result);
        line.getChildren().addAll(label, field, copy);
        calculationsContainer.getChildren().add(line);
    }

    private Button makeButton(BigDecimal result) {
        Button copy = new Button();
        copy.setGraphic(SVGUtils.makeCopyIcon());
        copy.setUserData(result);
        copy.setOnAction(this::copy);
        return copy;
    }

    private static TextField makeField(BigDecimal result) {
        TextField field = new TextField(ParseUtils.formatBigDecimal(result));
        field.setEditable(false);
        field.getStyleClass().add("calculate-text");
        return field;
    }

    private static Label makeLabel(String description) {
        Label label = new Label(description);
        label.setAlignment(Pos.CENTER_RIGHT);
        label.setTextAlignment(TextAlignment.RIGHT);
        label.setPrefWidth(120.0);
        label.getStyleClass().add("calculate-text");
        return label;
    }

    private static HBox makeLine() {
        HBox line = new HBox();
        line.setAlignment(Pos.CENTER_LEFT);
        line.setSpacing(5.0);
        return line;
    }

    private void calculatePlannedAndActual() {
        Label heading = new Label("Planned");
        heading.getStyleClass().add("calculate-heading-text");
        calculationsContainer.getChildren().add(heading);
        BigDecimal plannedSum = planned.stream().filter(CalculateDlgController::isValid).reduce(BigDecimal.ZERO, BigDecimal::add);
        addCalculation("Sum", plannedSum);
        BigDecimal plannedMonthlyPayment = plannedSum.divide(new BigDecimal(12), RoundingMode.DOWN);
        addCalculation("Sum / 12", plannedMonthlyPayment);
        BigDecimal plannedAnnualPayment = plannedSum.multiply(new BigDecimal(12));
        addCalculation("Sum x 12", plannedAnnualPayment);
        BigDecimal plannedAverage = plannedSum.divide(BigDecimal.valueOf(planned.size()), RoundingMode.DOWN);
        addCalculation("Average", plannedAverage);
        heading = new Label("Actual");
        heading.getStyleClass().add("calculate-heading-text");
        calculationsContainer.getChildren().add(heading);
        BigDecimal sumActual = actual.stream().filter(CalculateDlgController::isValid).reduce(BigDecimal.ZERO, BigDecimal::add);
        addCalculation("Sum", sumActual);
        BigDecimal actualMonthlyPayment = sumActual.divide(new BigDecimal(12), RoundingMode.DOWN);
        addCalculation("Sum / 12", actualMonthlyPayment);
        BigDecimal actualAnnualPayment = sumActual.multiply(new BigDecimal(12));
        addCalculation("Sum x 12", actualAnnualPayment);
        BigDecimal actualAverage = plannedSum.divide(BigDecimal.valueOf(actual.size()), RoundingMode.DOWN);
        addCalculation("Average", actualAverage);
        heading = new Label("Planned vs. Actual");
        heading.getStyleClass().add("calculate-heading-text");
        calculationsContainer.getChildren().add(heading);
        BigDecimal difference = plannedSum.subtract(sumActual);
        addCalculation("Difference", difference);
        BigDecimal percentage = sumActual.divide(plannedSum, RoundingMode.HALF_EVEN).multiply(new BigDecimal("100.0"));
        addCalculation("Percentage", percentage);
    }

    private static boolean isValid(BigDecimal bigDecimal) {
        return bigDecimal != null && bigDecimal.abs(MathContext.UNLIMITED).compareTo(BigDecimal.ZERO) > 0;
    }

    private void copy(ActionEvent actionEvent) {
        ClipboardContent content = new ClipboardContent();
        content.putString(ParseUtils.formatBigDecimalSimple((BigDecimal) ((Node) actionEvent.getSource()).getUserData()));
        Clipboard.getSystemClipboard().setContent(content);
    }

}
