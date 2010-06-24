package org.smallmind.nutsnbolts.swing.dialog;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
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
import org.smallmind.nutsnbolts.swing.file.DirectoryManager;

public class DirectoryManagerDialog extends JDialog implements WindowListener {

   private static final GridBagLayout GRID_BAG_LAYOUT = new GridBagLayout();
   private static final FlowLayout FLOW_LAYOUT = new FlowLayout(FlowLayout.RIGHT);

   private List<File> internalDirectoryList;
   private List<File> externalDirectoryList;
   private CancelAction cancelAction;
   boolean initialized = false;

   public static void createShowDialog (Dialog parentDialog, List<File> externalDirectoryList) {

      DirectoryManagerDialog directoryChooserDialog;

      directoryChooserDialog = new DirectoryManagerDialog(parentDialog, externalDirectoryList);
      directoryChooserDialog.showDialog();
   }

   public static void createShowDialog (Frame parentFrame, List<File> externalDirectoryList) {

      DirectoryManagerDialog directoryChooserDialog;

      directoryChooserDialog = new DirectoryManagerDialog(parentFrame, externalDirectoryList);
      directoryChooserDialog.showDialog();
   }

   public DirectoryManagerDialog (Dialog parentDialog, List<File> externalDirectoryList) {

      super(parentDialog, "Manage Directories...");

      this.externalDirectoryList = externalDirectoryList;
      internalDirectoryList = new ArrayList<File>(externalDirectoryList);

      buildDialog(parentDialog, new DirectoryManager(parentDialog, internalDirectoryList));
   }

   public DirectoryManagerDialog (Frame parentFrame, List<File> externalDirectoryList) {

      super(parentFrame, "Manage Directories...");

      this.externalDirectoryList = externalDirectoryList;
      internalDirectoryList = new ArrayList<File>(externalDirectoryList);

      buildDialog(parentFrame, new DirectoryManager(parentFrame, internalDirectoryList));
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
