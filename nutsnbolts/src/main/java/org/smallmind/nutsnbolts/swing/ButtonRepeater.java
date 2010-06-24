package org.smallmind.nutsnbolts.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class ButtonRepeater implements ChangeListener {

   private WeakEventListenerList<ActionListener> listenerList;
   private JButton button;
   private AutoPress autoPress;
   private boolean armed = false;
   private long delayMilliseconds;

   public ButtonRepeater (JButton button, long delayMilliseconds) {

      Thread autoThread;

      this.button = button;
      this.delayMilliseconds = delayMilliseconds;

      autoPress = new AutoPress();

      autoThread = new Thread(autoPress);
      autoThread.start();

      button.addChangeListener(this);

      listenerList = new WeakEventListenerList<ActionListener>();
   }

   public synchronized void addActionListener (ActionListener actionListener) {

      listenerList.addListener(actionListener);
   }

   public synchronized void removeActionListener (ActionListener actionListener) {

      listenerList.removeListener(actionListener);
   }

   public synchronized void stateChanged (ChangeEvent changeEvent) {

      if (!armed) {
         if (button.getModel().isPressed()) {
            armed = true;
            fireActionPereformed(new ActionEvent(button, 0, button.getActionCommand()));
         }
      }
      else if (!button.getModel().isPressed()) {
         armed = false;
      }
   }

   public void fireActionPereformed (ActionEvent actionEvent) {

      for (ActionListener actionListener : listenerList) {
         actionListener.actionPerformed(actionEvent);
      }
   }

   public void finalize () {

      autoPress.finish();
   }

   private class DoClick implements Runnable {

      public void run () {

         fireActionPereformed(new ActionEvent(button, 0, button.getActionCommand()));
      }
   }

   private class AutoPress implements Runnable {

      private Thread runnableThread;
      private boolean finished = false;
      private boolean exited = false;

      public void finish () {

         finished = true;

         while (!exited) {
            runnableThread.interrupt();

            try {
               Thread.sleep(100);
            }
            catch (InterruptedException interrupedException) {
            }
         }
      }

      public void run () {

         runnableThread = Thread.currentThread();

         while (!finished) {
            try {
               if (!armed) {
                  Thread.sleep(100);
               }
               else {
                  Thread.sleep(delayMilliseconds);
                  if (armed) {
                     SwingUtilities.invokeLater(new DoClick());
                  }
               }
            }
            catch (InterruptedException interrupedException) {
            }
         }

         exited = true;
      }
   }

}
