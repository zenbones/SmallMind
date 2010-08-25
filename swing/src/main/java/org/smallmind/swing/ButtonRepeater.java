package org.smallmind.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;
import org.smallmind.scribe.pen.LoggerManager;

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

      try {
         autoPress.finish();
      }
      catch (InterruptedException interruptedException) {
         LoggerManager.getLogger(ButtonRepeater.class).error(interruptedException);
      }
   }

   private class DoClick implements Runnable {

      public void run () {

         fireActionPereformed(new ActionEvent(button, 0, button.getActionCommand()));
      }
   }

   private class AutoPress implements Runnable {

      private CountDownLatch exitLatch;
      private CountDownLatch pulseLatch;
      private AtomicBoolean finished = new AtomicBoolean(false);

      public AutoPress () {

         pulseLatch = new CountDownLatch(1);
         exitLatch = new CountDownLatch(1);
      }

      public void finish ()
         throws InterruptedException {

         if (finished.compareAndSet(false, true)) {
            pulseLatch.countDown();
         }

         exitLatch.await();
      }

      public void run () {

         while (!finished.get()) {
            try {
               if (!armed) {
                  pulseLatch.await(100, TimeUnit.MILLISECONDS);
               }
               else {
                  pulseLatch.await(delayMilliseconds, TimeUnit.MILLISECONDS);
                  if (armed) {
                     SwingUtilities.invokeLater(new DoClick());
                  }
               }
            }
            catch (InterruptedException interruptedException) {
               LoggerManager.getLogger(ButtonRepeater.class).error(interruptedException);
            }
         }

         exitLatch.countDown();
      }
   }
}
