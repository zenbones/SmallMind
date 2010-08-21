package org.smallmind.swing.dialog;

import java.awt.Dialog;
import java.awt.Frame;

public class StopDialog extends OptionDialog {

   public static void showStopDialog (Frame parentFrame, String stoptext) {

      StopDialog stopDialog = new StopDialog(parentFrame, stoptext);

      stopDialog.setModal(true);
      stopDialog.setVisible(true);
   }

   public static void showStopDialog (Dialog parentDialog, String stoptext) {

      StopDialog stopDialog = new StopDialog(parentDialog, stoptext);

      stopDialog.setModal(true);
      stopDialog.setVisible(true);
   }

   public StopDialog (Frame parentFrame, String stoptext) {

      super(parentFrame, stoptext, OptionType.STOP);
   }

   public StopDialog (Dialog parentDialog, String stoptext) {

      super(parentDialog, stoptext, OptionType.STOP);
   }

}
