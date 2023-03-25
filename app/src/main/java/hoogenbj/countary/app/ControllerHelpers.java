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

import hoogenbj.countary.model.AllocationHolder;
import hoogenbj.countary.model.BudgetHolder;
import hoogenbj.countary.model.DataModel;
import hoogenbj.countary.model.Transaction;
import hoogenbj.countary.util.DbUtils;
import hoogenbj.countary.util.ParseUtils;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface ControllerHelpers {

    default Node getLabeledNode(String s) {
        Label label = new Label(s);
        label.setAlignment(Pos.CENTER);
        Font bigFont = new Font(18);
        label.setFont(bigFont);
        anchorLayout(label);
        return label;
    }

    default <S> TableCell<S, String> makeStringCell(TableColumn<S, String> tableColumn) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!empty && item != null && !item.isEmpty()) {
                    setText(item);
                    setTooltip(new Tooltip(item));
                }
            }
        };
    }

    default <S> TableCell<S, BigDecimal> makeBigDecimalCell(TableColumn<S, BigDecimal> tableColumn) {
        return new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);
                if (!empty && item != null) {
                    setText(ParseUtils.formatBigDecimal(item));
                    setTooltip(new Tooltip(item.toString()));
                }
            }
        };
    }

    default <S, T> TableCell<S, T> makeDeleteButton(TableColumn<S, T> tableColumn, Function<S, Future<Button>> buttonGraphicMaker) {
        return new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                this.setText(null);
                this.setGraphic(null);
                if (!empty && item != null) {
                    try {
                        S itemHolder = getTableRow().getItem();
                        this.setGraphic(buttonGraphicMaker.apply(itemHolder).get());
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException("Unable to get button from future", e);
                    }
                }
            }
        };
    }

    default void anchorLayout(Node node) {
        AnchorPane.setTopAnchor(node, 0.0);
        AnchorPane.setLeftAnchor(node, 0.0);
        AnchorPane.setBottomAnchor(node, 0.0);
        AnchorPane.setRightAnchor(node, 0.0);
    }

    default Node makeBusyIndicator() {
        ProgressIndicator busy = new ProgressIndicator();
        busy.setMaxHeight(50);
        busy.setMaxWidth(50);
        VBox vbox = new VBox(busy);
        vbox.setAlignment(Pos.CENTER);
        anchorLayout(vbox);
        return vbox;
    }

    default void manageCustomColors(Settings settings, ColorPicker colorPicker) {
        String customColors = settings.getCustomColors();
        if (!customColors.isEmpty()) {
            String[] colors = customColors.split(",");
            Arrays.stream(colors).toList().forEach(color -> colorPicker.getCustomColors().add(Color.web(color)));
        }
        colorPicker.getCustomColors().addListener((ListChangeListener<? super Color>) change -> {
            if (change.getList().size() > 0) {
                String colors = String.join(",", change.getList().stream().map(ParseUtils::toRGBCode).toList());
                settings.setCustomColors(colors);
            }
        });
    }
}
