package org.smallmind.nutsnbolts.swing.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import org.smallmind.nutsnbolts.swing.ComponentUtilities;
import org.smallmind.nutsnbolts.swing.LayoutManagerConstructionException;
import org.smallmind.nutsnbolts.swing.LayoutManagerFactory;
import org.smallmind.nutsnbolts.swing.event.DialogEvent;
import org.smallmind.nutsnbolts.swing.event.DialogListener;
import org.smallmind.nutsnbolts.swing.panel.OptionPanel;

public class ProgressOptionPanel extends OptionPanel implements ProgressOperator, DialogListener {

   private JProgressBar progressBar;
   private JLabel processLabel;
   private ProgressRunnable progressRunnable;
   private boolean withLabel;
   private boolean closeOnComplete;

   public ProgressOptionPanel (ProgressRunnable progressRunnable, int orientation, int min, int max, boolean withLabel, boolean closeOnComplete)
      throws LayoutManagerConstructionException {

      super(LayoutManagerFactory.getLayoutManager(GridBagLayout.class));

      GridBagConstraints constraint;

      this.progressRunnable = progressRunnable;
      this.withLabel = withLabel;
      this.closeOnComplete = closeOnComplete;

      if (withLabel) {
         processLabel = new JLabel("X");
         ComponentUtilities.setPreferredHeight(processLabel, processLabel.getPreferredSize().height);
         processLabel.setText("");
      }

      progressBar = new JProgressBar(orientation, min, max);
      progressBar.setStringPainted(true);
      progressBar.setBorderPainted(true);

      constraint = new GridBagConstraints();

      if (withLabel) {
         constraint.anchor = GridBagConstraints.WEST;
         constraint.fill = GridBagConstraints.HORIZONTAL;
         constraint.insets = new Insets(0, 0, 0, 0);
         constraint.gridx = 0;
         constraint.gridy = 0;
         constraint.weightx = 1;
         constraint.weighty = 0;
         add(processLabel, constraint);
      }

      constraint.anchor = GridBagConstraints.WEST;
      constraint.fill = GridBagConstraints.HORIZONTAL;
      constraint.insets = new Insets((!withLabel) ? 0 : 3, 0, 0, 0);
      constraint.gridx = 0;
      constraint.gridy = (!withLabel) ? 0 : 1;
      constraint.weightx = 1;
      constraint.weighty = 0;
      add(progressBar, constraint);
   }

   public void initalize (OptionDialog optionDialog) {

      Thread progressThread;

      super.initalize(optionDialog);

      progressRunnable.initalize(this);
      progressThread = new Thread(progressRunnable);
      progressThread.start();

      optionDialog.addDialogListener(this);
   }

   public String validateOption (DialogState dialogState) {

      return null;
   }

   public synchronized void setProcessLabel (String name) {

      if (!withLabel) {
         throw new IllegalStateException("Progress bar has been requested without a label");
      }

      processLabel.setText(name);
   }

   public synchronized void setMinimum (int min) {

      progressBar.setMinimum(min);
   }

   public synchronized void setMaximum (int max) {

      progressBar.setMaximum(max);
   }

   public synchronized void setValue (int value) {

      progressBar.setValue(value);
      if (value == progressBar.getMaximum()) {
         if (closeOnComplete) {
            setDialogSate(DialogState.COMPLETE);
            closeParent();
         }
         else {
            getOptionDialog().replaceButtons(new OptionButton[] {new OptionButton("Continue", DialogState.CONTINUE)});
         }
      }
   }

   public synchronized void dialogHandler (DialogEvent dialogEvent) {

      if (!getDialogState().equals(DialogState.COMPLETE)) {
         progressRunnable.terminate();
      }
   }

}
