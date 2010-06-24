package org.smallmind.nutsnbolts.swing.spinner;

public class IntegerSpinnerEditor extends DefaultSpinnerEditor {

   private IntegerSpinnerModel model;

   public IntegerSpinnerEditor (IntegerSpinnerModel model) {

      super();

      this.model = model;
   }

   public boolean isValid () {

      int value;

      try {
         value = Integer.parseInt((String)super.getValue());
      }
      catch (NumberFormatException numberFormatException) {
         return false;
      }

      if ((model.getMinimumValue() != null) && (value < (Integer)model.getMinimumValue())) {
         return false;
      }
      else {
         return !((model.getMaximumValue() != null) && (value > (Integer)model.getMaximumValue()));
      }
   }

   public Object getValue () {

      return Integer.parseInt((String)super.getValue());
   }

}
