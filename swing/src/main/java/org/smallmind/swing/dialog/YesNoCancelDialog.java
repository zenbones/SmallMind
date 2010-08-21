package org.smallmind.swing.dialog;

import java.awt.Dialog;
import java.awt.Frame;

public class YesNoCancelDialog extends OptionDialog {

   private static final OptionButton[] yesNoCancelButtons = {new OptionButton("Yes", DialogState.YES), new OptionButton("No", DialogState.NO), new OptionButton("Cancel", DialogState.CANCEL)};

   public static DialogState showYesNoCancelDialog (Frame parentFrame, String yesNoText) {

      YesNoCancelDialog yesNoCancelDialog = new YesNoCancelDialog(parentFrame, yesNoText);

      yesNoCancelDialog.setModal(true);
      yesNoCancelDialog.setVisible(true);
      return yesNoCancelDialog.getDialogState();
   }

   public static DialogState showYesNoCancelDialog (Dialog parentDialog, String yesNoText) {

      YesNoCancelDialog yesNoCancelDialog = new YesNoCancelDialog(parentDialog, yesNoText);

      yesNoCancelDialog.setModal(true);
      yesNoCancelDialog.setVisible(true);
      return yesNoCancelDialog.getDialogState();
   }

   public YesNoCancelDialog (Frame parentFrame, String yesNoText) {

      super(parentFrame, yesNoText, OptionType.QUESTION, yesNoCancelButtons);
   }

   public YesNoCancelDialog (Dialog parentDialog, String yesNoText) {

      super(parentDialog, yesNoText, OptionType.QUESTION, yesNoCancelButtons);
   }

}
