/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.swing.file;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

public class DirectoryManagerDialog extends JDialog implements WindowListener {

  private static final GridBagLayout GRID_BAG_LAYOUT = new GridBagLayout();
  private static final FlowLayout FLOW_LAYOUT = new FlowLayout(FlowLayout.RIGHT);

  private List<File> internalDirectoryList;
  private List<File> externalDirectoryList;
  private CancelAction cancelAction;
  boolean initialized = false;

  public static void createShowDialog (Window parentWindow, List<File> externalDirectoryList) {

    DirectoryManagerDialog directoryChooserDialog;

    directoryChooserDialog = new DirectoryManagerDialog(parentWindow, externalDirectoryList);
    directoryChooserDialog.showDialog();
  }

  public DirectoryManagerDialog (Window parentWindow, List<File> externalDirectoryList) {

    super(parentWindow, "Manage Directories...");

    this.externalDirectoryList = externalDirectoryList;
    internalDirectoryList = new ArrayList<File>(externalDirectoryList);

    buildDialog(parentWindow, new DirectoryManager(parentWindow, internalDirectoryList));
  }

  private void buildDialog (Window parentWindow, DirectoryManager directoryManager) {

    GridBagConstraints constraints = new GridBagConstraints();
    Container contentPane;
    JPanel buttonPanel;
    JButton okButton;
    JButton cancelButton;
    OKAction okAction;

    setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

    contentPane = getContentPane();
    contentPane.setLayout(GRID_BAG_LAYOUT);

    okAction = new OKAction();
    cancelAction = new CancelAction();

    okButton = new JButton(okAction);
    okButton.registerKeyboardAction(okAction, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

    cancelButton = new JButton(cancelAction);
    cancelButton.registerKeyboardAction(cancelAction, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

    buttonPanel = new JPanel(FLOW_LAYOUT);
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);

    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.insets = new Insets(5, 5, 0, 5);
    constraints.weightx = 1;
    constraints.weighty = 1;
    contentPane.add(directoryManager, constraints);

    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.insets = new Insets(5, 5, 5, 5);
    constraints.weightx = 1;
    constraints.weighty = 0;
    contentPane.add(buttonPanel, constraints);

    pack();
    setLocationRelativeTo(parentWindow);
    setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

    addWindowListener(this);
  }

  public void showDialog () {

    setModal(true);
    setVisible(true);
  }

  public void windowOpened (WindowEvent windowEvent) {

  }

  public synchronized void windowClosing (WindowEvent windowEvent) {

    cancelAction.actionPerformed(null);
  }

  public void windowClosed (WindowEvent windowEvent) {

  }

  public void windowIconified (WindowEvent windowEvent) {

  }

  public void windowDeiconified (WindowEvent windowEvent) {

  }

  public void windowActivated (WindowEvent windowEvent) {

  }

  public void windowDeactivated (WindowEvent windowEvent) {

  }

  public class OKAction extends AbstractAction {

    public OKAction () {

      super();

      putValue(Action.NAME, "OK");
    }

    public synchronized void actionPerformed (ActionEvent actionEvent) {

      externalDirectoryList.clear();
      externalDirectoryList.addAll(internalDirectoryList);

      setVisible(false);
      dispose();
    }

  }

  public class CancelAction extends AbstractAction {

    public CancelAction () {

      super();

      putValue(Action.NAME, "Cancel");
    }

    public synchronized void actionPerformed (ActionEvent actionEvent) {

      setVisible(false);
      dispose();
    }
  }
}
