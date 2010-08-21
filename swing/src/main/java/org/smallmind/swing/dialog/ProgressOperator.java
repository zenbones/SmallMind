package org.smallmind.swing.dialog;

public interface ProgressOperator {

   public abstract OptionDialog getOptionDialog ();

   public abstract void setProcessLabel (String name);

   public abstract void setMinimum (int min);

   public abstract void setMaximum (int max);

   public abstract void setValue (int value);

}
