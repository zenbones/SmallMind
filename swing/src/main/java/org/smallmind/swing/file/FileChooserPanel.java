/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import org.smallmind.nutsnbolts.awt.ColorUtilities;
import org.smallmind.nutsnbolts.util.StringUtilities;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;
import org.smallmind.swing.SmallMindGrayFilter;
import org.smallmind.swing.dialog.DialogState;
import org.smallmind.swing.dialog.OptionButton;
import org.smallmind.swing.dialog.OptionDialog;
import org.smallmind.swing.dialog.OptionType;
import org.smallmind.swing.dialog.WarningDialog;
import org.smallmind.swing.dialog.YesNoDialog;
import org.smallmind.swing.event.FileChoiceEvent;
import org.smallmind.swing.event.FileChoiceListener;
import org.smallmind.swing.panel.OptionPanel;

public class FileChooserPanel extends JPanel implements ComponentListener, MouseListener, KeyListener, ActionListener, ListSelectionListener {

  private static final ImageIcon NEW_FOLDER_ICON = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("org/smallmind/swing/system/folder_new_16.png"));
  private static final ImageIcon EDIT_FOLDER_ICON = new ImageIcon(Thread.currentThread().getContextClassLoader().getResource("org/smallmind/swing/system/folder_edit_16.png"));

  private final WeakEventListenerList<FileChoiceListener> listenerList = new WeakEventListenerList<FileChoiceListener>();

  private Window parentWindow;
  private FileChooserState state;
  private JTable directoryTable;
  private JList filePickList;
  private DirectoryTableModel directoryTableModel;
  private FilePickListModel filePickListModel;
  private JComboBox filterComboBox;
  private FilterComboBoxModel filterComboBoxModel;
  private JButton newFolderButton;
  private JButton editFolderButton;
  private JButton chooseButton;
  private JButton cancelButton;
  private JScrollPane directoryTableScrollPane;
  private JTextField fileNameTextField;
  private File chosenFile;
  private AtomicBoolean selectionSensitive = new AtomicBoolean(true);

  public FileChooserPanel (Window parentWindow, FileChooserState state) {

    this(parentWindow, state, null, null);
  }

  public FileChooserPanel (Window parentWindow, FileChooserState state, File directory) {

    this(parentWindow, state, directory, null);
  }

  public FileChooserPanel (Window parentWindow, FileChooserState state, File directory, FileFilter filter) {

    super();

    this.parentWindow = parentWindow;
    this.state = state;

    GroupLayout layout;
    GroupLayout.SequentialGroup topBarSequentialGroup;
    GroupLayout.SequentialGroup fileNameSequentialGroup;
    GroupLayout.SequentialGroup buttonSequentialGroup;
    GroupLayout.ParallelGroup topBarParallelGroup;
    GroupLayout.ParallelGroup fileNameParallelGroup;
    GroupLayout.ParallelGroup buttonParallelGroup;
    DirectoryTableCellRenderer directoryTableCellRenderer;
    JLabel fileLabel;
    JLabel filterLabel;
    JScrollPane filePickListScrollPane;
    int directoryTableHeight;
    int fileNameTextFieldHeight;
    int fileLabelWidth;
    int filterLabelWidth;

    setLayout(layout = new GroupLayout(this));

    newFolderButton = new JButton(NEW_FOLDER_ICON);
    newFolderButton.setFocusable(false);
    newFolderButton.setToolTipText("create a new folder");
    newFolderButton.addActionListener(this);

    editFolderButton = new JButton(EDIT_FOLDER_ICON);
    editFolderButton.setDisabledIcon(new ImageIcon(SmallMindGrayFilter.createDisabledImage(EDIT_FOLDER_ICON.getImage())));
    editFolderButton.setEnabled(false);
    editFolderButton.setFocusable(false);
    editFolderButton.setToolTipText("rename a document or folder");
    editFolderButton.addActionListener(this);

    chooseButton = new JButton(StringUtilities.toDisplayCase(state.name(), '_'));
    chooseButton.setFocusable(false);
    chooseButton.addActionListener(this);

    cancelButton = new JButton("Cancel");
    cancelButton.setFocusable(false);
    cancelButton.addActionListener(this);

    filterComboBox = new JComboBox(filterComboBoxModel = new FilterComboBoxModel(filter));
    filterComboBox.setEditable(false);
    filterComboBox.setRenderer(new FilterListCellRenderer());
    filterComboBox.setBackground(ColorUtilities.TEXT_COLOR);
    filterComboBox.setFocusable(false);
    filterComboBox.addActionListener(this);

    directoryTable = new JTable(directoryTableModel = new DirectoryTableModel(directory));
    directoryTable.setDefaultRenderer(File.class, directoryTableCellRenderer = new DirectoryTableCellRenderer());
    directoryTable.setShowGrid(false);
    directoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    directoryTable.setFillsViewportHeight(true);
    directoryTable.setAutoscrolls(false);
    directoryTable.setRowSelectionAllowed(false);
    directoryTable.setColumnSelectionAllowed(false);
    directoryTable.setCellSelectionEnabled(true);
    directoryTable.getColumnModel().setColumnMargin(0);
    directoryTable.addComponentListener(this);
    directoryTable.addMouseListener(this);

    filePickList = new JList(filePickListModel = new FilePickListModel(directory, filter));
    filePickList.setCellRenderer(new FilePickListCellRenderer());
    filePickList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    filePickList.setLayoutOrientation(JList.VERTICAL_WRAP);
    filePickList.addComponentListener(this);
    filePickList.addMouseListener(this);
    filePickList.addKeyListener(this);
    filePickList.addListSelectionListener(this);

    fileNameTextField = new JTextField();

    fileLabel = new JLabel("File:");
    filterLabel = new JLabel("Filter:");

    directoryTableHeight = (int)directoryTableCellRenderer.getTableCellRendererComponent(directoryTable, directoryTableModel.getValueAt(0, 0), false, false, 0, 0).getPreferredSize().getHeight();
    directoryTable.setRowMargin((directoryTableHeight / 2) * -1);
    fileNameTextFieldHeight = (int)fileNameTextField.getPreferredSize().getHeight() + 2;
    fileLabelWidth = (int)fileLabel.getPreferredSize().getWidth();
    filterLabelWidth = (int)filterLabel.getPreferredSize().getWidth();

    directoryTableScrollPane = new JScrollPane(directoryTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    directoryTableScrollPane.getViewport().setBackground(directoryTable.getBackground());
    directoryTableScrollPane.addComponentListener(this);

    filePickListScrollPane = new JScrollPane(filePickList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    layout.setAutoCreateContainerGaps(true);

    topBarSequentialGroup = layout.createSequentialGroup().addComponent(directoryTableScrollPane).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(newFolderButton).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(editFolderButton);
    topBarParallelGroup = layout.createParallelGroup().addComponent(directoryTableScrollPane, directoryTableHeight, directoryTableHeight, directoryTableHeight).addComponent(newFolderButton, directoryTableHeight, directoryTableHeight, directoryTableHeight).addComponent(editFolderButton, directoryTableHeight, directoryTableHeight, directoryTableHeight);
    fileNameSequentialGroup = layout.createSequentialGroup().addComponent(fileLabel, fileLabelWidth, fileLabelWidth, fileLabelWidth).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(fileNameTextField).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(filterLabel, filterLabelWidth, filterLabelWidth, filterLabelWidth).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(filterComboBox, 200, 200, 200);
    fileNameParallelGroup = layout.createParallelGroup().addComponent(fileLabel, fileNameTextFieldHeight, fileNameTextFieldHeight, fileNameTextFieldHeight).addComponent(fileNameTextField, fileNameTextFieldHeight, fileNameTextFieldHeight, fileNameTextFieldHeight).addComponent(filterLabel, fileNameTextFieldHeight, fileNameTextFieldHeight, fileNameTextFieldHeight).addComponent(filterComboBox, fileNameTextFieldHeight, fileNameTextFieldHeight, fileNameTextFieldHeight);
    buttonSequentialGroup = layout.createSequentialGroup().addComponent(chooseButton).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(cancelButton);
    buttonParallelGroup = layout.createParallelGroup().addComponent(chooseButton).addComponent(cancelButton);

    layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING).addGroup(topBarSequentialGroup).addComponent(filePickListScrollPane).addGroup(fileNameSequentialGroup).addGroup(buttonSequentialGroup));
    layout.setVerticalGroup(layout.createSequentialGroup().addGroup(topBarParallelGroup).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(filePickListScrollPane).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(fileNameParallelGroup).addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(buttonParallelGroup));
  }

  public File getChosenFile () {

    return chosenFile;
  }

  public FileFilter getFilter () {

    return filePickListModel.getFilter();
  }

  public void setFilter (FileFilter filter) {

    filterComboBoxModel.setFilter(filter);
  }

  private void finishingTouches () {

    File originallyChosenFile = chosenFile;

    if ((chosenFile == null) && (fileNameTextField.getText() != null) && (fileNameTextField.getText().length() > 0)) {
      chosenFile = new File(directoryTableModel.getDirectory(), fileNameTextField.getText());
    }
    if ((chosenFile != null) && (!chosenFile.getName().equals(fileNameTextField.getText().trim()))) {
      if ((fileNameTextField.getText() == null) || (fileNameTextField.getText().length() == 0)) {
        chosenFile = null;
      }
      else if (fileNameTextField.getText().indexOf(System.getProperty("file.separator")) >= 0) {
        chosenFile = new File(fileNameTextField.getText());
      }
      else {
        chosenFile = new File(chosenFile.getParentFile(), fileNameTextField.getText());
      }
    }

    if (state.equals(FileChooserState.SAVE) && (chosenFile != null) && chosenFile.exists()) {
      if (YesNoDialog.showYesNoDialog(parentWindow, OptionType.WARNING, chosenFile.getName() + " already exists. Overwite the file?").equals(DialogState.YES)) {
        fireFileChosen(new FileChoiceEvent(this, chosenFile));
      }
      else {
        chosenFile = originallyChosenFile;
      }
    }
    else {
      fireFileChosen(new FileChoiceEvent(this, chosenFile));
    }
  }

  @Override
  public synchronized void valueChanged (ListSelectionEvent listSelectionEvent) {

    if (selectionSensitive.get()) {
      directoryTable.clearSelection();
      editFolderButton.setEnabled(true);

      if (!((File)filePickListModel.getElementAt(filePickList.getSelectedIndex())).isDirectory()) {
        chosenFile = (File)filePickListModel.getElementAt(filePickList.getSelectedIndex());
        fileNameTextField.setText(((File)filePickListModel.getElementAt(filePickList.getSelectedIndex())).getName());
      }
      else {
        chosenFile = null;
        fileNameTextField.setText("");
      }
    }
  }

  @Override
  public void actionPerformed (ActionEvent actionEvent) {

    if ((actionEvent.getSource() == filterComboBox) && ((filterComboBox.getSelectedItem() != null) || (filePickListModel.getFilter() != null)) && (((filterComboBox.getSelectedItem() != null) && (!filterComboBox.getSelectedItem().equals(filePickListModel.getFilter()))) || ((filePickListModel.getFilter() != null) && (!filePickListModel.getFilter().equals(filterComboBox.getSelectedItem()))))) {

      selectionSensitive.set(false);
      filePickList.clearSelection();
      selectionSensitive.set(true);

      filePickListModel.setFilter((FileFilter)filterComboBox.getSelectedItem());

      chosenFile = null;
      fileNameTextField.setText("");
    }
    else if (actionEvent.getSource() == newFolderButton) {

      boolean success;
      int count = 0;

      do {
        success = new File(directoryTableModel.getDirectory(), (count == 0) ? "New file" : "New file (" + count + ")").mkdir();
        count++;
      } while (!success);

      filePickListModel.setDirectory(filePickListModel.getDirectory());
    }
    else if (actionEvent.getSource() == editFolderButton) {

      final File selectedFile = (File)filePickList.getSelectedValue();
      final JTextField renameTextField = new JTextField(selectedFile.getName());

      OptionDialog renameDialog;
      OptionPanel renamePanel = new OptionPanel(new BorderLayout()) {

        @Override
        public String validateOption (DialogState dialogState) {

          if (dialogState.equals(DialogState.APPLY) && (!renameTextField.getText().trim().equals(selectedFile.getName()))) {
            if ((renameTextField.getText() == null) || (renameTextField.getText().trim().length() == 0)) {

              return "You need to fill in a name";
            }
            if (new File(selectedFile.getParentFile(), renameTextField.getText().trim()).exists()) {

              return "That name is already in use";
            }

            if (selectedFile.renameTo(new File(selectedFile.getParentFile(), renameTextField.getText().trim()))) {
              selectionSensitive.set(false);
              filePickList.clearSelection();
              selectionSensitive.set(true);

              if (!selectedFile.isDirectory()) {
                chosenFile = null;
                fileNameTextField.setText("");
              }

              filePickListModel.setDirectory(filePickListModel.getDirectory());
            }
          }

          return null;
        }
      };

      renamePanel.add(renameTextField);
      renameTextField.setColumns(25);

      renameDialog = new OptionDialog(parentWindow, "Rename " + (((File)filePickList.getSelectedValue()).isDirectory() ? "folder" : "document") + "...", OptionType.QUESTION, new OptionButton[] {new OptionButton("Apply", DialogState.APPLY), new OptionButton("Cancel", DialogState.CANCEL)}, renamePanel);
      renameDialog.setModal(true);
      renameDialog.setVisible(true);
    }
    else if (actionEvent.getSource() == chooseButton) {
      finishingTouches();
    }
    else if (actionEvent.getSource() == cancelButton) {
      fireFileChosen(new FileChoiceEvent(this, null));
    }
  }

  @Override
  public void keyTyped (KeyEvent keyEvent) {

  }

  @Override
  public void keyPressed (KeyEvent keyEvent) {

  }

  @Override
  public void keyReleased (KeyEvent keyEvent) {

    int selectedIndex;

    if ((keyEvent.getKeyCode() == KeyEvent.VK_DELETE) && (keyEvent.getModifiers() == 0) && ((selectedIndex = filePickList.getSelectedIndex()) >= 0)) {

      File vanishingFile;

      if (((vanishingFile = (File)filePickListModel.getElementAt(selectedIndex)).isDirectory()) && (vanishingFile.listFiles().length > 0)) {
        WarningDialog.showWarningDialog(parentWindow, "Directories must be empty before deletion");
      }
      else if (vanishingFile.delete()) {
        filePickListModel.setDirectory(filePickListModel.getDirectory());
        if (filePickListModel.getSize() == 0) {
          editFolderButton.setEnabled(false);
        }
      }
    }
  }

  @Override
  public void mouseClicked (MouseEvent mouseEvent) {

    Object selectedValue;

    if (mouseEvent.getSource() == directoryTable) {
      selectionSensitive.set(false);
      filePickList.clearSelection();
      selectionSensitive.set(true);

      editFolderButton.setEnabled(false);
      chosenFile = null;
      fileNameTextField.setText("");

      if ((mouseEvent.getButton() == MouseEvent.BUTTON1) && (mouseEvent.getClickCount() == 2) && (directoryTable.getSelectedColumn() < directoryTableModel.getColumnCount() - 1)) {

        File directory = (File)directoryTableModel.getValueAt(0, directoryTable.getSelectedColumn());

        directoryTableModel.setDirectory(directory);
        directoryTable.createDefaultColumnsFromModel();
        filePickListModel.setDirectory(directory);
      }
    }
    else if (mouseEvent.getSource() == filePickList) {
      if ((mouseEvent.getButton() == MouseEvent.BUTTON1) && (mouseEvent.getClickCount() == 2) && ((selectedValue = filePickList.getSelectedValue()) != null)) {
        selectionSensitive.set(false);
        filePickList.clearSelection();
        selectionSensitive.set(true);

        if (((File)selectedValue).isDirectory()) {
          directoryTableModel.setDirectory((File)selectedValue);
          directoryTable.createDefaultColumnsFromModel();
          filePickListModel.setDirectory((File)selectedValue);
          editFolderButton.setEnabled(false);
          chosenFile = null;
          fileNameTextField.setText("");
        }
        else {
          finishingTouches();
        }
      }
    }
  }

  @Override
  public void mousePressed (MouseEvent mouseEvent) {

  }

  @Override
  public void mouseReleased (MouseEvent mouseEvent) {

  }

  @Override
  public void mouseEntered (MouseEvent mouseEvent) {

  }

  @Override
  public void mouseExited (MouseEvent mouseEvent) {

  }

  @Override
  public void componentResized (ComponentEvent componentEvent) {

    if ((componentEvent.getSource() == directoryTable) || (componentEvent.getSource() == directoryTableScrollPane)) {
      directoryTable.scrollRectToVisible(directoryTable.getCellRect(0, directoryTableModel.getColumnCount() - 1, true));
    }
    else if (componentEvent.getSource() == filePickList) {
      filePickList.setVisibleRowCount(Math.max(10, (int)(filePickList.getVisibleRect().getHeight() / ((FilePickListCellRenderer)filePickList.getCellRenderer()).getRowHeight())));
    }
  }

  @Override
  public void componentMoved (ComponentEvent componentEvent) {

  }

  @Override
  public void componentShown (ComponentEvent componentEvent) {

  }

  @Override
  public void componentHidden (ComponentEvent componentEvent) {

  }

  private void fireFileChosen (FileChoiceEvent fileChoiceEvent) {

    for (FileChoiceListener fileChoiceListener : listenerList) {
      fileChoiceListener.fileChosen(fileChoiceEvent);
    }
  }

  public synchronized void addFileChoiceListener (FileChoiceListener fileChoiceListener) {

    listenerList.addListener(fileChoiceListener);
  }

  public synchronized void removeFileChoiceListener (FileChoiceListener fileChoiceListener) {

    listenerList.removeListener(fileChoiceListener);
  }
}
