package org.smallmind.swing.dialog;

import java.awt.Dialog;
import java.awt.Frame;

public class ProgressDialog extends OptionDialog {

   public static void showProgressDialog (Frame parentFrame, String title, ProgressOptionPanel progressOptionPanel) {

      ProgressDialog progressDialog = new ProgressDialog(parentFrame, title, progressOptionPanel);

      progressDialog.setModal(true);
      progressDialog.setVisible(true);
   }

   public static void showProgressDialog (Dialog parentDialog, String title, ProgressOptionPanel progressOptionPanel) {

      ProgressDialog progressDialog = new ProgressDialog(parentDialog, title, progressOptionPanel);

      progressDialog.setModal(true);
      progressDialog.setVisible(true);
   }

   public ProgressDialog (Frame parentFrame, String title, ProgressOptionPanel progressOptionPanel) {

      super(parentFrame, title, OptionType.PROGRESS, new OptionButton[] {new OptionButton("Cancel", DialogState.CANCEL)}, progressOptionPanel);
   }

   public ProgressDialog (Dialog parentDilaog, String title, ProgressOptionPanel progressOptionPanel) {

      super(parentDilaog, title, OptionType.PROGRESS, new OptionButton[] {new OptionButton("Cancel", DialogState.CANCEL)}, progressOptionPanel);
   }

}
