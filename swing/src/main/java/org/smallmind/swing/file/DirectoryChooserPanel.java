/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
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

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.LayoutStyle;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;
import org.smallmind.swing.tree.SmallMindTreeModel;

public class DirectoryChooserPanel extends JPanel implements ActionListener, ItemListener, TreeSelectionListener, DocumentListener {

  private static final GridBagLayout GRID_BAG_LAYOUT = new GridBagLayout();
  private static final Dimension PREFERRED_DIMENSION = new Dimension(300, 500);

  private WeakEventListenerList<DirectoryChoiceListener> listenerList = new WeakEventListenerList<DirectoryChoiceListener>();
  private JTree directoryTree;
  private JTextField directoryTextField;
  private JButton cancelButton;
  private JButton selectButton;

  public DirectoryChooserPanel () {

    super(GRID_BAG_LAYOUT);

    GroupLayout layout;
    JScrollPane directoryTreeScrollPane;
    JComboBox rootComboBox;
    JLabel rootLabel;
    JLabel directoryLabel;
    LinkedList<File> rootList;

    setLayout(layout = new GroupLayout(this));

    rootList = new LinkedList<File>();
    for (File root : File.listRoots()) {
      if (root.isDirectory()) {
        rootList.add(root);
      }
    }

    rootLabel = new JLabel("Root:");
    directoryLabel = new JLabel("Directory:");

    rootComboBox = new JComboBox(rootList.toArray());
    rootComboBox.setEditable(false);
    rootComboBox.setRenderer(new RootListCellRenderer());

    selectButton = new JButton("Select");
    selectButton.setFocusable(false);
    selectButton.addActionListener(this);

    cancelButton = new JButton("Cancel");
    cancelButton.setFocusable(false);
    cancelButton.addActionListener(this);

    directoryTree = new JTree();
    setRoot((File)rootComboBox.getSelectedItem());

    directoryTree.setEditable(false);
    directoryTree.setRootVisible(true);
    directoryTree.setShowsRootHandles(true);
    directoryTree.setScrollsOnExpand(false);
    directoryTree.setCellRenderer(new DirectoryTreeCellRenderer());
    directoryTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

    directoryTreeScrollPane = new JScrollPane(directoryTree);

    directoryTextField = new JTextField();

    layout.setAutoCreateContainerGaps(true);

    layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
      .addGroup(layout.createSequentialGroup().addComponent(rootLabel).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(rootComboBox))
      .addComponent(directoryTreeScrollPane)
      .addGroup(layout.createSequentialGroup().addComponent(directoryLabel).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(directoryTextField))
      .addGroup(layout.createSequentialGroup().addComponent(selectButton).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(cancelButton)));

    layout.setVerticalGroup(layout.createSequentialGroup()
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(rootLabel).addComponent(rootComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
      .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(directoryTreeScrollPane).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(directoryLabel).addComponent(directoryTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
      .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(layout.createParallelGroup().addComponent(selectButton).addComponent(cancelButton)));

    rootComboBox.addItemListener(this);
    directoryTextField.getDocument().addDocumentListener(this);
    directoryTree.getSelectionModel().addTreeSelectionListener(this);
  }

  public Dimension getPreferredSize () {

    return PREFERRED_DIMENSION;
  }

  public synchronized void addDirectoryChoiceListener (DirectoryChoiceListener directoryChoiceListener) {

    listenerList.addListener(directoryChoiceListener);
  }

  public synchronized void removeDirectoryChoiceListener (DirectoryChoiceListener directoryChoiceListener) {

    listenerList.removeListener(directoryChoiceListener);
  }

  private void setRoot (File file) {

    directoryTree.setModel(new SmallMindTreeModel(new DirectoryNode(new Directory(file.getAbsolutePath()))));
  }

  private synchronized void fireDirectoryChosen (DirectoryChoiceEvent directoryChoiceEvent) {

    Iterator<DirectoryChoiceListener> listenerIter = listenerList.getListeners();

    while (listenerIter.hasNext()) {
      listenerIter.next().directoryChosen(directoryChoiceEvent);
    }
  }

  public void actionPerformed (ActionEvent actionEvent) {

    if (actionEvent.getSource() == selectButton) {
      if ((directoryTextField.getText() != null) && (directoryTextField.getText().trim().length() > 0)) {
        fireDirectoryChosen(new DirectoryChoiceEvent(this, new File(directoryTextField.getText().trim())));
      }
      else {
        fireDirectoryChosen(new DirectoryChoiceEvent(this, null));
      }
    }
    else if (actionEvent.getSource() == cancelButton) {
      fireDirectoryChosen(new DirectoryChoiceEvent(this, null));
    }
  }

  public synchronized void itemStateChanged (ItemEvent itemEvent) {

    File rootFile;

    directoryTextField.setText("");

    if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
      rootFile = (File)itemEvent.getItem();
      setRoot(rootFile);
    }
  }

  public synchronized void valueChanged (TreeSelectionEvent treeSelectionEvent) {

    Directory directory;

    directory = (Directory)((DirectoryNode)treeSelectionEvent.getPath().getLastPathComponent()).getUserObject();

    directoryTextField.getDocument().removeDocumentListener(this);
    directoryTextField.setText(directory.getAbsolutePath());
    directoryTextField.getDocument().addDocumentListener(this);
  }

  private void shiftDirectory () {

    if (directoryTree.getSelectionModel().getSelectionPath() != null) {
      directoryTree.getSelectionModel().removeTreeSelectionListener(this);
      directoryTree.getSelectionModel().clearSelection();
      directoryTree.getSelectionModel().addTreeSelectionListener(this);
    }
  }

  public synchronized void insertUpdate (DocumentEvent documentEvent) {

    shiftDirectory();
  }

  public synchronized void removeUpdate (DocumentEvent documentEvent) {

    shiftDirectory();
  }

  public synchronized void changedUpdate (DocumentEvent documentEvent) {

    shiftDirectory();
  }
}
