package org.smallmind.swing.dialog;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Iterator;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import org.smallmind.swing.event.DialogEvent;
import org.smallmind.swing.event.DialogListener;
import org.smallmind.swing.panel.WizardPanel;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class WizardDialog extends JDialog implements WindowListener {

   private static final GridBagLayout GRID_BAG_LAYOUT = new GridBagLayout();
   private static final GridLayout GRID_LAYOUT = new GridLayout(1, 0);
   private static final FlowLayout FLOW_LAYOUT = new FlowLayout(FlowLayout.RIGHT);

   private Window parentWindow;
   private Object result;
   private WeakEventListenerList<DialogListener> listenerList;
   private JTabbedPane wizardTabbedPane;
   private JPanel headerPanel;
   private CancelAction cancelAction;
   private WizardResultValidator validator;
   private boolean parentIsFrame;

   public WizardDialog (Dialog parentDialog, String title, Object result) {

      super(parentDialog, title);

      parentIsFrame = false;
      construct(parentDialog, result);
   }

   public WizardDialog (Frame parentFrame, String title, Object result) {

      super(parentFrame, title);

      parentIsFrame = true;
      construct(parentFrame, result);
   }

   private void construct (Window parentWindow, Object result) {

      GridBagConstraints constraint = new GridBagConstraints();
      Container contentPane;
      JPanel buttonPanel;
      JButton okButton;
      JButton cancelButton;
      OKAction okAction;

      this.parentWindow = parentWindow;
      this.result = result;

      setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

      contentPane = getContentPane();
      contentPane.setLayout(GRID_BAG_LAYOUT);

      headerPanel = new JPanel(GRID_LAYOUT);

      wizardTabbedPane = new JTabbedPane();
      wizardTabbedPane.setFocusable(false);

      okAction = new OKAction();
      cancelAction = new CancelAction();

      okButton = new JButton(okAction);
      okButton.registerKeyboardAction(okAction, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

      cancelButton = new JButton(cancelAction);
      cancelButton.registerKeyboardAction(cancelAction, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

      buttonPanel = new JPanel(FLOW_LAYOUT);
      buttonPanel.add(okButton);
      buttonPanel.add(cancelButton);
      buttonPanel.add(new JButton(new ApplyAction()));

      constraint.anchor = GridBagConstraints.NORTHWEST;
      constraint.fill = GridBagConstraints.HORIZONTAL;
      constraint.insets = new Insets(0, 0, 0, 0);
      constraint.gridx = 0;
      constraint.gridy = 0;
      constraint.weightx = 1;
      constraint.weighty = 0;
      contentPane.add(headerPanel, constraint);

      constraint.anchor = GridBagConstraints.NORTHWEST;
      constraint.fill = GridBagConstraints.BOTH;
      constraint.insets = new Insets(0, 0, 0, 0);
      constraint.gridx = 0;
      constraint.gridy = 1;
      constraint.weightx = 1;
      constraint.weighty = 1;
      contentPane.add(wizardTabbedPane, constraint);

      constraint.anchor = GridBagConstraints.NORTHWEST;
      constraint.fill = GridBagConstraints.HORIZONTAL;
      constraint.insets = new Insets(0, 0, 0, 0);
      constraint.gridx = 0;
      constraint.gridy = 2;
      constraint.weightx = 1;
      constraint.weighty = 0;
      contentPane.add(buttonPanel, constraint);

      pack();
      setResizable(false);
      setLocationRelativeTo(parentWindow);
      setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

      addWindowListener(this);

      listenerList = new WeakEventListenerList<DialogListener>();
   }

   public Object getResult () {

      return result;
   }

   public synchronized void addDialogListener (DialogListener listener) {

      listenerList.addListener(listener);
   }

   public synchronized void removeDialogListener (DialogListener listener) {

      listenerList.removeListener(listener);
   }

   public void setValidator (WizardResultValidator validator) {

      this.validator = validator;
   }

   public void setHeader (Component component, int gap) {

      JPanel gapPanel;

      headerPanel.removeAll();

      gapPanel = new JPanel(new GridLayout(1, 0));
      gapPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, gap, 0));
      gapPanel.add(component);

      headerPanel.add(gapPanel);
   }

   public void addWizardPanel (WizardPanel wizardPanel) {

      wizardTabbedPane.addTab(wizardPanel.getTitle(), wizardPanel);
      wizardPanel.setWizardDialog(this);
   }

   public void displayWarning (String warningMessage) {

      WarningDialog warningDialog;

      if (parentIsFrame) {
         warningDialog = new WarningDialog((Frame)parentWindow, warningMessage);
      }
      else {
         warningDialog = new WarningDialog((Dialog)parentWindow, warningMessage);
      }

      warningDialog.setModal(true);
      warningDialog.setVisible(true);
   }

   public synchronized void fireDialogHandler (DialogState dialogState) {

      Iterator<DialogListener> listenerIter = listenerList.getListeners();
      DialogEvent dialogEvent;

      dialogEvent = new DialogEvent(this, dialogState);
      while (listenerIter.hasNext()) {
         listenerIter.next().dialogHandler(dialogEvent);
      }
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

         String invalidationMessage;

         if ((validator != null) && ((invalidationMessage = validator.isValid(getResult())) != null)) {
            displayWarning(invalidationMessage);
         }
         else {
            setVisible(false);
            dispose();
            fireDialogHandler(DialogState.OK);
         }
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
         fireDialogHandler(DialogState.CANCEL);
      }

   }

   public class ApplyAction extends AbstractAction {

      public ApplyAction () {

         super();

         putValue(Action.NAME, "Apply");
      }

      public synchronized void actionPerformed (ActionEvent actionEvent) {

         String invalidationMessage;

         if ((validator != null) && ((invalidationMessage = validator.isValid(getResult())) != null)) {
            displayWarning(invalidationMessage);
         }
         else {
            fireDialogHandler(DialogState.APPLY);
         }
      }
   }

}
