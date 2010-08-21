package org.smallmind.swing.panel;

import java.awt.LayoutManager;
import javax.swing.JPanel;
import org.smallmind.swing.dialog.DialogState;
import org.smallmind.swing.dialog.OptionDialog;

public abstract class OptionPanel extends JPanel {

   private OptionDialog optionDialog;
   private boolean initialized = false;

   public OptionPanel (LayoutManager layoutManager) {

      super(layoutManager);
   }

   public OptionDialog getOptionDialog () {

      return optionDialog;
   }

   public DialogState getDialogState () {

      return optionDialog.getDialogState();
   }

   public void setDialogSate (DialogState dialogState) {

      optionDialog.setDialogState(dialogState);
   }

   public void initalize (OptionDialog optionDialog) {

      this.optionDialog = optionDialog;

      initialized = true;
   }

   public void closeParent () {

      if (!initialized) {
         throw new IllegalStateException("Parent dialog was never initalized");
      }

      optionDialog.windowClosing(null);
   }

   public abstract String validateOption (DialogState dialogState);

}
