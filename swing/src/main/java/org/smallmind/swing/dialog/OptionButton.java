package org.smallmind.swing.dialog;

public class OptionButton {

   private String buttonName;
   private DialogState buttonState;

   public OptionButton (String buttonName, DialogState buttonState) {

      this.buttonName = buttonName;
      this.buttonState = buttonState;
   }

   public String getName () {

      return buttonName;
   }

   public DialogState getButtonState () {

      return buttonState;
   }

}
