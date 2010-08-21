package org.smallmind.swing.memory;

import java.awt.GridLayout;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import org.smallmind.swing.event.MemoryUsageEvent;
import org.smallmind.swing.event.MemoryUsageListener;

public class MemoryPanel extends JPanel implements MemoryUsageListener {

   private JProgressBar progressBar;

   public MemoryPanel () {

      super(new GridLayout(1, 0));

      progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
      progressBar.setMinimum(0);
      progressBar.setStringPainted(true);
      progressBar.setString("");

      add(progressBar);
   }

   public void usageUpdate (MemoryUsageEvent memeoryUsageEvent) {

      progressBar.setMaximum(memeoryUsageEvent.getMaximumUsage());
      progressBar.setValue(memeoryUsageEvent.getCurrentUsage());
      progressBar.setString(memeoryUsageEvent.getDisplayUsage());
   }

}
