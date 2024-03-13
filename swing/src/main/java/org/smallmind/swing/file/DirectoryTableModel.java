/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.swing.file;

import java.io.File;
import java.util.ArrayList;
import jakarta.swing.event.TableModelListener;
import jakarta.swing.table.TableModel;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class DirectoryTableModel implements TableModel {

  private final WeakEventListenerList<TableModelListener> listenerList = new WeakEventListenerList<TableModelListener>();
  private File directory;
  private ArrayList<File> pathList;

  public DirectoryTableModel (File directory) {

    setDirectory(directory);
  }

  public File getDirectory () {

    return directory;
  }

  public void setDirectory (File directory) {

    File pathElement;

    this.directory = (directory == null) ? new File(System.getProperty("user.home")) : directory;

    pathList = new ArrayList<File>();
    pathElement = this.directory;
    do {
      pathList.add(0, pathElement);
    } while ((pathElement = pathElement.getParentFile()) != null);
  }

  @Override
  public int getRowCount () {

    return 1;
  }

  @Override
  public int getColumnCount () {

    return pathList.size();
  }

  @Override
  public String getColumnName (int columnIndex) {

    return null;
  }

  @Override
  public Class<?> getColumnClass (int columnIndex) {

    return File.class;
  }

  @Override
  public boolean isCellEditable (int rowIndex, int columnIndex) {

    return false;
  }

  @Override
  public Object getValueAt (int rowIndex, int columnIndex) {

    return ((!pathList.isEmpty()) && (columnIndex < pathList.size())) ? pathList.get(columnIndex) : null;
  }

  @Override
  public void setValueAt (Object aValue, int rowIndex, int columnIndex) {

  }

  @Override
  public synchronized void addTableModelListener (TableModelListener tableModelListener) {

    listenerList.addListener(tableModelListener);
  }

  @Override
  public synchronized void removeTableModelListener (TableModelListener tableModelListener) {

    listenerList.removeListener(tableModelListener);
  }
}
