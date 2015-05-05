/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the Apache License, Version 2.0.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
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
