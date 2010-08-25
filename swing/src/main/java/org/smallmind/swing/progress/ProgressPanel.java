package org.smallmind.swing.progress;

import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import org.smallmind.scribe.pen.LoggerManager;

public class ProgressPanel extends JPanel {

   private ProgressTimer progressTimer;
   private ProgressDataHandler dataHandler;
   private JProgressBar progressBar;

   public ProgressPanel (ProgressDataHandler dataHandler, long pulseTime) {

      super(new GridLayout(1, 0));

      this.dataHandler = dataHandler;

      Thread timerThread;

      progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
      progressBar.setMinimum(0);
      progressBar.setMaximum(100);
      progressBar.setStringPainted(true);
      progressBar.setString("");

      add(progressBar);

      progressTimer = new ProgressTimer(this, pulseTime);
      timerThread = new Thread(progressTimer);
      timerThread.setDaemon(true);
      timerThread.start();
   }

   public void setProgress () {

      long percentageProgress;

      percentageProgress = (dataHandler.getIndex() * 100) / dataHandler.getLength();
      progressBar.setValue((int)percentageProgress);
      progressBar.setString(String.valueOf(percentageProgress) + "%");
   }

   public void finalize () {

      try {
         progressTimer.finish();
      }
      catch (InterruptedException interruptedException) {
         LoggerManager.getLogger(ProgressPanel.class).error(interruptedException);
      }
   }
}
