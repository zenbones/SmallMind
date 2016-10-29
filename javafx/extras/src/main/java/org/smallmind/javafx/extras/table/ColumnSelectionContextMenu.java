/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the Apache License, Version 2.0.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.javafx.extras.table;

import java.util.Collections;
import java.util.LinkedList;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import org.smallmind.javafx.extras.Selectable;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.nutsnbolts.util.AlphaNumericComparator;
import org.smallmind.nutsnbolts.util.AlphaNumericConverter;

public class ColumnSelectionContextMenu extends ContextMenu {

  private static final AlphaNumericComparator<MenuItem> MENU_ITEM_COMPARATOR = new AlphaNumericComparator<>(new AlphaNumericConverter<MenuItem>() {

    @Override
    public String toString (MenuItem menuItem) {

      return menuItem.getText();
    }
  });

  public ColumnSelectionContextMenu (TableColumn... columns) {

    super(constructMenuItems(columns));

    for (TableColumn column : columns) {
      column.setContextMenu(this);
    }
  }

  private static MenuItem[] constructMenuItems (TableColumn... columns) {

    CheckMenuItem[] menuItems;
    LinkedList<CheckMenuItem> menuItemList = new LinkedList<>();

    for (final TableColumn column : columns) {
      if (column instanceof Selectable) {
        switch (((Selectable)column).getSelectionHint()) {
          case ALWAYS:
            break;
          case ON:
            menuItemList.add(new CheckMenuItem(column.getText()));
            menuItemList.getLast().setSelected(true);
            menuItemList.getLast().selectedProperty().addListener(new ChangeListener<Boolean>() {

              @Override
              public void changed (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

                column.setVisible(newValue);
              }
            });
            break;
          case OFF:
            column.setVisible(false);
            menuItemList.add(new CheckMenuItem(column.getText()));
            menuItemList.getLast().selectedProperty().addListener(new ChangeListener<Boolean>() {

              @Override
              public void changed (ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {

                column.setVisible(newValue);
              }
            });
            break;
          default:
            throw new UnknownSwitchCaseException(((Selectable)column).getSelectionHint().name());
        }
      }
    }

    Collections.sort(menuItemList, MENU_ITEM_COMPARATOR);
    menuItems = new CheckMenuItem[menuItemList.size()];
    menuItemList.toArray(menuItems);

    return menuItems;
  }
}
