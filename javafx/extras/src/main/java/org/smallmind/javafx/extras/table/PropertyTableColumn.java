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
 * A {@link TableColumn} that reads its value from a named JavaBean property of the row model class
 * and optionally formats the value using a {@link StringConverter}. Cell content is aligned
 * according to a configurable {@link Pos}. The converter, if supplied, is applied whenever the
 * cell is not in edit mode.
 *
 * @param <S> the type of the table row model
 * @param <T> the type of the property value displayed in this column
 */
public class PropertyTableColumn<S, T> extends TableColumn<S, T> {

  private final String propertyName;

  /**
   * Creates a column with the given header text and property name, using no converter and
   * {@link Pos#CENTER_LEFT} alignment.
   *
   * @param text         the column header text; must not be {@code null}
   * @param propertyName the name of the JavaBean property to display; must not be {@code null}
   */
  public PropertyTableColumn (String text, String propertyName) {

    this(text, propertyName, null, Pos.CENTER_LEFT);
  }

  /**
   * Creates a column with the given header text, property name, and converter, using
   * {@link Pos#CENTER_LEFT} alignment.
   *
   * @param text         the column header text; must not be {@code null}
   * @param propertyName the name of the JavaBean property to display; must not be {@code null}
   * @param converter    the converter used to format the property value for display; may be {@code null}
   */
  public PropertyTableColumn (String text, String propertyName, StringConverter<T> converter) {

    this(text, propertyName, converter, Pos.CENTER_LEFT);
  }

  /**
   * Creates a column with the given header text, property name, and cell alignment, using no
   * converter.
   *
   * @param text         the column header text; must not be {@code null}
   * @param propertyName the name of the JavaBean property to display; must not be {@code null}
   * @param position     the alignment to apply to each cell; must not be {@code null}
   */
  public PropertyTableColumn (String text, String propertyName, Pos position) {

    this(text, propertyName, null, position);
  }

  /**
   * Creates a fully configured column. A custom cell factory is installed that applies
   * {@code converter} when rendering non-empty, non-editing cells, and sets the cell alignment
   * to {@code position}. Cell values are sourced from {@code propertyName} via a
   * {@link PropertyValueFactory}.
   *
   * @param text         the column header text; must not be {@code null}
   * @param propertyName the name of the JavaBean property to display; must not be {@code null}
   * @param converter    the converter used to format the property value; {@code null} leaves
   *                     text rendering to the default cell behaviour
   * @param position     the alignment to apply to each cell; must not be {@code null}
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
   * Returns the name of the JavaBean property that supplies values to this column.
   *
   * @return the property name; never {@code null}
   */
  public String getPropertyName () {

    return propertyName;
  }
}
