package org.smallmind.nutsnbolts.swing.file;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;
import org.smallmind.nutsnbolts.swing.SmallMindScrollPane;
import org.smallmind.nutsnbolts.swing.event.DirectoryChoiceEvent;
import org.smallmind.nutsnbolts.swing.event.DirectoryChoiceListener;
import org.smallmind.nutsnbolts.swing.tree.SmallMindTreeModel;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class DirectoryChooser extends JPanel implements ItemListener, TreeSelectionListener, DocumentListener {

   private static final GridBagLayout GRID_BAG_LAYOUT = new GridBagLayout();
   private static final Dimension PREFERRED_DIMENSION = new Dimension(300, 500);

   private WeakEventListenerList<DirectoryChoiceListener> listenerList;
   private JTree directoryTree;
   private JTextField directoryTextField;

   public DirectoryChooser () {

      super(GRID_BAG_LAYOUT);

      GridBagConstraints constraints = new GridBagConstraints();
      JPanel rootPanel;
      JPanel inputPanel;
      JScrollPane directoryTreeScrollPane;
      JComboBox rootComboBox;
      LinkedList<File> rootList;

      listenerList = new WeakEventListenerList<DirectoryChoiceListener>();

      rootList = new LinkedList<File>();
      for (File root : File.listRoots()) {
         if (root.isDirectory()) {
            rootList.add(root);
         }
      }

      rootComboBox = new JComboBox(rootList.toArray());
      rootComboBox.setEditable(false);
      rootComboBox.setRenderer(new RootListCellRenderer());

      directoryTree = new JTree();
      setRoot((File)rootComboBox.getSelectedItem());

      directoryTree.setEditable(false);
      directoryTree.setRootVisible(true);
      directoryTree.setShowsRootHandles(true);
      directoryTree.setScrollsOnExpand(false);
      directoryTree.setCellRenderer(new DirectoryTreeCellRenderer());
      directoryTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

      directoryTreeScrollPane = new SmallMindScrollPane(directoryTree);

      directoryTextField = new JTextField();

      rootPanel = new JPanel(GRID_BAG_LAYOUT);
      inputPanel = new JPanel(GRID_BAG_LAYOUT);

      constraints.anchor = GridBagConstraints.WEST;
      constraints.fill = GridBagConstraints.NONE;
      constraints.insets = new Insets(0, 0, 0, 0);
      constraints.gridx = 0;
      constraints.gridy = 0;
      constraints.weightx = 0;
      constraints.weighty = 0;
      rootPanel.add(new JLabel("Root:"), constraints);

      constraints.anchor = GridBagConstraints.WEST;
      constraints.fill = GridBagConstraints.NONE;
      constraints.insets = new Insets(0, 5, 0, 0);
      constraints.gridx = 1;
      constraints.gridy = 0;
      constraints.weightx = 1;
      constraints.weighty = 0;
      rootPanel.add(rootComboBox, constraints);

      constraints.fill = GridBagConstraints.NONE;
      constraints.insets = new Insets(0, 0, 0, 0);
      constraints.gridx = 0;
      constraints.gridy = 0;
      constraints.weightx = 0;
      constraints.weighty = 0;
      inputPanel.add(new JLabel("Directory:"), constraints);

      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.insets = new Insets(0, 5, 0, 0);
      constraints.gridx = 1;
      constraints.gridy = 0;
      constraints.weightx = 1;
      constraints.weighty = 0;
      inputPanel.add(directoryTextField, constraints);

      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.insets = new Insets(0, 0, 0, 0);
      constraints.gridx = 0;
      constraints.gridy = 0;
      constraints.weightx = 1;
      constraints.weighty = 0;
      add(rootPanel, constraints);

      constraints.fill = GridBagConstraints.BOTH;
      constraints.insets = new Insets(8, 0, 0, 0);
      constraints.gridx = 0;
      constraints.gridy = 1;
      constraints.weightx = 1;
      constraints.weighty = 1;
      add(directoryTreeScrollPane, constraints);

      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.insets = new Insets(8, 0, 0, 0);
      constraints.gridx = 0;
      constraints.gridy = 2;
      constraints.weightx = 1;
      constraints.weighty = 0;
      add(inputPanel, constraints);

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

   private synchronized void fireRootChosen (DirectoryChoiceEvent directoryChoiceEvent) {

      Iterator<DirectoryChoiceListener> listenerIter = listenerList.getListeners();

      while (listenerIter.hasNext()) {
         listenerIter.next().rootChosen(directoryChoiceEvent);
      }
   }

   private synchronized void fireDirectoryChosen (DirectoryChoiceEvent directoryChoiceEvent) {

      Iterator<DirectoryChoiceListener> listenerIter = listenerList.getListeners();

      while (listenerIter.hasNext()) {
         listenerIter.next().directoryChosen(directoryChoiceEvent);
      }
   }

   public synchronized void itemStateChanged (ItemEvent itemEvent) {

      File rootFile;

      directoryTextField.setText("");

      if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
         rootFile = (File)itemEvent.getItem();
         setRoot(rootFile);
         fireRootChosen(new DirectoryChoiceEvent(this, rootFile));
      }
   }

   public synchronized void valueChanged (TreeSelectionEvent treeSelectionEvent) {

      Directory directory;

      directory = (Directory)((DirectoryNode)treeSelectionEvent.getPath().getLastPathComponent()).getUserObject();

      directoryTextField.getDocument().removeDocumentListener(this);
      directoryTextField.setText(directory.getAbsolutePath());
      directoryTextField.getDocument().addDocumentListener(this);

      fireDirectoryChosen(new DirectoryChoiceEvent(this, directory));
   }

   private void shiftDirectory () {

      if (directoryTree.getSelectionModel().getSelectionPath() != null) {
         directoryTree.getSelectionModel().removeTreeSelectionListener(this);
         directoryTree.getSelectionModel().clearSelection();
         directoryTree.getSelectionModel().addTreeSelectionListener(this);
      }

      fireDirectoryChosen(new DirectoryChoiceEvent(this, new File(directoryTextField.getText())));
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
