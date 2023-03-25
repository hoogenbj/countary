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

import hoogenbj.countary.model.CategoryLiteHolder;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;

import java.lang.ref.WeakReference;

public class DefaultTreeTagTreeCell extends TreeCell<CategoryLiteHolder> {
    private WeakReference<TreeItem<CategoryLiteHolder>> treeItemRef;

    private final InvalidationListener treeItemGraphicListener = observable -> {
        updateDisplay(getItem(), isEmpty());
    };

    private final WeakInvalidationListener weakTreeItemGraphicListener =
            new WeakInvalidationListener(treeItemGraphicListener);

    public DefaultTreeTagTreeCell() {
        InvalidationListener treeItemListener = new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                TreeItem<CategoryLiteHolder> oldTreeItem = treeItemRef == null ? null : treeItemRef.get();
                if (oldTreeItem != null) {
                    oldTreeItem.graphicProperty().removeListener(weakTreeItemGraphicListener);
                }

                TreeItem<CategoryLiteHolder> newTreeItem = getTreeItem();
                if (newTreeItem != null) {
                    newTreeItem.graphicProperty().addListener(weakTreeItemGraphicListener);
                    treeItemRef = new WeakReference<TreeItem<CategoryLiteHolder>>(newTreeItem);
                }
            }
        };
        WeakInvalidationListener weakTreeItemListener = new WeakInvalidationListener(treeItemListener);
        treeItemProperty().addListener(weakTreeItemListener);

        if (getTreeItem() != null) {
            getTreeItem().graphicProperty().addListener(weakTreeItemGraphicListener);
        }
    }

    void updateDisplay(CategoryLiteHolder item, boolean empty) {
        if (item == null || empty) {
            setText(null);
            setGraphic(null);
        } else {
            // update the graphic if one is set in the TreeItem
            TreeItem<CategoryLiteHolder> treeItem = getTreeItem();
            if (treeItem != null && treeItem.getGraphic() != null) {
                setText(item.toString());
                setGraphic(treeItem.getGraphic());
            } else {
                setText(item.toString());
                setGraphic(null);
            }
        }
    }

    @Override
    public void updateItem(CategoryLiteHolder item, boolean empty) {
        super.updateItem(item, empty);
        updateDisplay(item, empty);
    }

}
