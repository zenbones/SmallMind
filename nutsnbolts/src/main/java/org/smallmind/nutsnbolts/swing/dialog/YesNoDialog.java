package org.smallmind.nutsnbolts.swing.dialog;

import java.awt.Dialog;
import java.awt.Frame;

public class YesNoDialog extends OptionDialog {

   private static final OptionButton[] yesNoButtons = {new OptionButton("Yes", DialogState.YES), new OptionButton("No", DialogState.NO)};

   public static DialogState showYesNoDialog (Frame parentFrame, String yesNoText) {

      YesNoDialog yesNoDialog = new YesNoDialog(parentFrame, yesNoText);

      yesNoDialog.setModal(true);
      yesNoDialog.setVisible(true);

      return yesNoDialog.getDialogState();
   }

   public static DialogState showYesNoDialog (Dialog parentDialog, String yesNoText) {

      YesNoDialog yesNoDialog = new YesNoDialog(parentDialog, yesNoText);

      yesNoDialog.setModal(true);
      yesNoDialog.setVisible(true);

      return yesNoDialog.getDialogState();
   }

   public YesNoDialog (Frame parentFrame, String yesNoText) {

      super(parentFrame, yesNoText, OptionType.QUESTION, yesNoButtons);
   }

   public YesNoDialog (Dialog parentDialog, String yesNoText) {

      super(parentDialog, yesNoText, OptionType.QUESTION, yesNoButtons);
   }

}
