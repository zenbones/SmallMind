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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

public class DirectoryManager extends JPanel {

  private static final GridBagLayout GRID_BAG_LAYOUT = new GridBagLayout();
  private static final Dimension PREFERRED_DIMENSION = new Dimension(300, 500);
  private static final ImageIcon DIRECTORY_ADD = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/application basics/24x24/plain/folder_add.png"));
  private static final ImageIcon DIRECTORY_REMOVE = new ImageIcon(ClassLoader.getSystemResource("public/iconexperience/application basics/24x24/plain/folder_delete.png"));
  private final JList directoryDisplayList;
  private final DirectoryManagerListModel listModel;
  private Window parentWindow;

  public DirectoryManager (Window parentWindow, List<File> directoryList) {

    this(directoryList);

    this.parentWindow = parentWindow;
  }

  private DirectoryManager (List<File> directoryList) {

    super(GRID_BAG_LAYOUT);

    Box buttonBox;
    JScrollPane directoryDisplayListScrollPane;
    JButton addDirectoryButton;
    JButton removeDirectoryButton;
    RemoveDirectoryAction removeDirectoryAction;

    GridBagConstraints constraints = new GridBagConstraints();

    listModel = new DirectoryManagerListModel(directoryList);

    directoryDisplayList = new JList(listModel);
    directoryDisplayList.setDragEnabled(false);
    directoryDisplayList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    directoryDisplayListScrollPane = new JScrollPane(directoryDisplayList);

    removeDirectoryAction = new RemoveDirectoryAction();
    removeDirectoryButton = new JButton(removeDirectoryAction);
    removeDirectoryButton.setFocusable(false);
    removeDirectoryButton.registerKeyboardAction(removeDirectoryAction, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

    if (listModel.getSize() == 0) {
      removeDirectoryAction.setEnabled(false);
    } else {
      directoryDisplayList.setSelectedIndex(0);
    }

    addDirectoryButton = new JButton(new AddDirectoryAction(removeDirectoryAction));
    addDirectoryButton.setFocusable(false);

    buttonBox = new Box(BoxLayout.Y_AXIS);
    buttonBox.add(addDirectoryButton);
    buttonBox.add(Box.createVerticalStrut(5));
    buttonBox.add(removeDirectoryButton);
    buttonBox.add(Box.createVerticalGlue());

    constraints.fill = GridBagConstraints.BOTH;
    constraints.insets = new Insets(0, 0, 0, 0);
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.weightx = 1;
    constraints.weighty = 1;
    add(directoryDisplayListScrollPane, constraints);

    constraints.fill = GridBagConstraints.BOTH;
    constraints.insets = new Insets(0, 5, 0, 0);
    constraints.gridx = 1;
    constraints.gridy = 0;
    constraints.weightx = 0;
    constraints.weighty = 1;
    add(buttonBox, constraints);
  }

  public Dimension getPreferredSize () {

    return PREFERRED_DIMENSION;
  }

  public class AddDirectoryAction extends AbstractAction {

    private final RemoveDirectoryAction removeDirectoryAction;

    public AddDirectoryAction (RemoveDirectoryAction removeDirectoryAction) {

      super();

      this.removeDirectoryAction = removeDirectoryAction;

      putValue(Action.SMALL_ICON, DIRECTORY_ADD);
      putValue(Action.SHORT_DESCRIPTION, "Add a media directory");
    }

    public synchronized void actionPerformed (ActionEvent actionEvent) {

      File addedDirectory;

      if ((addedDirectory = DirectoryChooserDialog.showDirectoryChooserDialog(parentWindow)) != null) {
        listModel.addDirectory(addedDirectory);
        removeDirectoryAction.setEnabled(true);

        if (directoryDisplayList.getSelectedIndex() < 0) {
          directoryDisplayList.setSelectedIndex(0);
        }
      }
    }
  }

  public class RemoveDirectoryAction extends AbstractAction {

    public RemoveDirectoryAction () {

      super();

      putValue(Action.SMALL_ICON, DIRECTORY_REMOVE);
      putValue(Action.SHORT_DESCRIPTION, "Remove a media directory");
    }

    public synchronized void actionPerformed (ActionEvent actionEvent) {

      int selectedIndex;

      listModel.removeDirectory(selectedIndex = directoryDisplayList.getSelectedIndex());

      if (listModel.getSize() == 0) {
        setEnabled(false);
      }
      if (selectedIndex < listModel.getSize()) {
        directoryDisplayList.setSelectedIndex(selectedIndex);
      } else {
        directoryDisplayList.setSelectedIndex(listModel.getSize() - 1);
      }
    }
  }
}
