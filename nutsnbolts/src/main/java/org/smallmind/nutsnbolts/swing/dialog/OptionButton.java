package org.smallmind.nutsnbolts.swing.dialog;

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
