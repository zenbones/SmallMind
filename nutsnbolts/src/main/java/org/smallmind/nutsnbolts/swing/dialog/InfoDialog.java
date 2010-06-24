package org.smallmind.nutsnbolts.swing.dialog;

import java.awt.Dialog;
import java.awt.Frame;

public class InfoDialog extends OptionDialog {

   public static void showInfoDialog (Frame parentFrame, String infoText) {

      InfoDialog infoDialog = new InfoDialog(parentFrame, infoText);

      infoDialog.setModal(true);
      infoDialog.setVisible(true);
   }

   public static void showInfoDialog (Dialog parentDialog, String infoText) {

      InfoDialog infoDialog = new InfoDialog(parentDialog, infoText);

      infoDialog.setModal(true);
      infoDialog.setVisible(true);
   }

   public InfoDialog (Frame parentFrame, String infoText) {

      super(parentFrame, infoText, OptionType.INFO);
   }

   public InfoDialog (Dialog parentDialog, String infoText) {

      super(parentDialog, infoText, OptionType.INFO);
   }

}
