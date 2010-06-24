package org.smallmind.nutsnbolts.swing.dialog;

public interface ProgressRunnable extends Runnable {

   public abstract void initalize (ProgressOperator progressOperator);

   public abstract void terminate ();

}
