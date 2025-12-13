/*
 * Copyright (c) 2007 through 2026 David Berkman
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

import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * {@link TableColumn} configured to display and optionally convert a named JavaBean property from the row item.
 *
 * @param <S> the table row type
 * @param <T> the property type
 */
public class PropertyTableColumn<S, T> extends TableColumn<S, T> {

  private final String propertyName;

  /**
   * Creates a column with the supplied header text and bean property name using default alignment.
   *
   * @param text         the column header
   * @param propertyName the name of the bean property to display
   */
  public PropertyTableColumn (String text, String propertyName) {

    this(text, propertyName, null, Pos.CENTER_LEFT);
  }

  /**
   * Creates a column with a custom converter.
   *
   * @param text         the column header
   * @param propertyName the name of the bean property to display
   * @param converter    converter used to render the property
   */
  public PropertyTableColumn (String text, String propertyName, StringConverter<T> converter) {

    this(text, propertyName, converter, Pos.CENTER_LEFT);
  }

  /**
   * Creates a column with a specific alignment.
   *
   * @param text         the column header
   * @param propertyName the name of the bean property to display
   * @param position     desired alignment for the cell content
   */
  public PropertyTableColumn (String text, String propertyName, Pos position) {

    this(text, propertyName, null, position);
  }

  /**
   * Creates a column with a converter and alignment, and sets a cell factory that applies the converter outside of
   * editing mode. The column value is sourced from the provided bean property name.
   *
   * @param text         the column header
   * @param propertyName the name of the bean property to display
   * @param converter    converter used to render the property (nullable)
   * @param position     desired alignment for the cell content
   */
  public PropertyTableColumn (String text, String propertyName, final StringConverter<T> converter, final Pos position) {

    super(text);

    setCellFactory(new Callback<TableColumn<S, T>, TableCell<S, T>>() {

      @Override
      public TableCell<S, T> call (TableColumn<S, T> param) {

        TableCell<S, T> tableCell = new TextFieldTableCell<S, T>() {

          @Override
          public void updateItem (T item, boolean empty) {

            super.updateItem(item, empty);

            if ((converter != null) && (!(empty || isEditing()))) {
              setText(converter.toString(item));
            }
          }
        };

        tableCell.setAlignment(position);

        return tableCell;
      }
    });

    setCellValueFactory(new PropertyValueFactory<S, T>(this.propertyName = propertyName));
  }

  /**
   * @return the bean property name used by the column
   */
  public String getPropertyName () {

    return propertyName;
  }
}
