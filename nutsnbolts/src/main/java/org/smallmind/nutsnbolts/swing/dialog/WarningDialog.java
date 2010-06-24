package org.smallmind.nutsnbolts.swing.dialog;

import java.awt.Dialog;
import java.awt.Frame;

public class WarningDialog extends OptionDialog {

   public static void showWarningDialog (Frame parentFrame, String warningText) {

      WarningDialog warningDialog = new WarningDialog(parentFrame, warningText);

      warningDialog.setModal(true);
      warningDialog.setVisible(true);
   }

   public static void showWarningDialog (Dialog parentDialog, String warningText) {

      WarningDialog warningDialog = new WarningDialog(parentDialog, warningText);

      warningDialog.setModal(true);
      warningDialog.setVisible(true);
   }

   public WarningDialog (Frame parentFrame, String warningText) {

      super(parentFrame, warningText, OptionType.WARNING);
   }

   public WarningDialog (Dialog parentDialog, String warningText) {

      super(parentDialog, warningText, OptionType.WARNING);
   }

}
