/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.swing.slider;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class DefaultMultiThumbModel implements MultiThumbModel {

  private final WeakEventListenerList<ThumbListener> listenerList = new WeakEventListenerList<ThumbListener>();
  private final AdjustingDelayHandler adjustingDelayHandler;

  private LinkedList<Integer> thumbList;
  private int minimumValue = 0;
  private int maximumValue = 100;

  public DefaultMultiThumbModel () {

    thumbList = new LinkedList<Integer>();

    new Thread(adjustingDelayHandler = new AdjustingDelayHandler(this)).start();
  }

  @Override
  public synchronized void setMinimumValue (int minimumValue) {

    this.minimumValue = minimumValue;
  }

  @Override
  public synchronized int getMinimumValue () {

    return minimumValue;
  }

  @Override
  public synchronized void setMaximumValue (int maximumValue) {

    this.maximumValue = maximumValue;
  }

  @Override
  public synchronized int getMaximumValue () {

    return maximumValue;
  }

  @Override
  public synchronized void addThumb (int thumbValue) {

    adjustingDelayHandler.purge();
    thumbList.add(thumbValue);
    fireThumbAdded(new ThumbEvent(this, ThumbEvent.EventType.ADD, thumbList.size() - 1, thumbValue, thumbValue, false));
  }

  @Override
  public synchronized void removeThumb (int thumbIndex) {

    int thumbValue;

    adjustingDelayHandler.purge();
    thumbValue = thumbList.remove(thumbIndex);
    fireThumbRemoved(new ThumbEvent(this, ThumbEvent.EventType.REMOVE, thumbIndex, thumbValue, thumbValue, false));
  }

  @Override
  public synchronized int getThumbCount () {

    return thumbList.size();
  }

  @Override
  public synchronized int[] getThumbValues () {

    int[] thumbValues = new int[thumbList.size()];
    int index = 0;

    for (Integer thumbValue : thumbList) {
      thumbValues[index++] = thumbValue;
    }

    Arrays.sort(thumbValues);

    return thumbValues;
  }

  @Override
  public synchronized int getThumbValue (int thumbIndex) {

    return thumbList.get(thumbIndex);
  }

  @Override
  public synchronized boolean moveThumb (int thumbIndex, int thumbValue) {

    int currentValue;

    currentValue = thumbList.set(thumbIndex, thumbValue);
    fireThumbMoved(new ThumbEvent(this, ThumbEvent.EventType.MOVE, thumbIndex, currentValue, thumbValue, true));
    adjustingDelayHandler.hold(thumbIndex, currentValue, thumbValue);

    return true;
  }

  private synchronized void fireThumbAdded (ThumbEvent thumbEvent) {

    for (ThumbListener thumbListener : listenerList) {
      thumbListener.thumbAdded(thumbEvent);
    }
  }

  private synchronized void fireThumbRemoved (ThumbEvent thumbEvent) {

    for (ThumbListener thumbListener : listenerList) {
      thumbListener.thumbRemoved(thumbEvent);
    }
  }

  private synchronized void fireThumbMoved (ThumbEvent thumbEvent) {

    for (ThumbListener thumbListener : listenerList) {
      thumbListener.thumbMoved(thumbEvent);
    }
  }

  @Override
  public synchronized void addThumbListener (ThumbListener thumbListener) {

    listenerList.addListener(thumbListener);
  }

  @Override
  public synchronized void removeThumbListener (ThumbListener thumbListener) {

    listenerList.removeListener(thumbListener);
  }

  private class AdjustingDelayHandler implements Runnable {

    private DefaultMultiThumbModel model;
    private CountDownLatch terminationLatch = new CountDownLatch(1);
    private CountDownLatch exitLatch = new CountDownLatch(1);
    private AtomicReference<DelayedMove> delayedMoveRef = new AtomicReference<DelayedMove>();

    public AdjustingDelayHandler (DefaultMultiThumbModel model) {

      this.model = model;
    }

    public synchronized void hold (int thumbIndex, int startingValue, int adjustingValue) {

      DelayedMove delayedMove;

      if ((delayedMove = delayedMoveRef.getAndSet(null)) != null) {
        if (delayedMove.getThumbIndex() == thumbIndex) {
          delayedMove.setAdjustingValue(adjustingValue);
          delayedMove.setTimestamp(System.currentTimeMillis());
          delayedMoveRef.set(delayedMove);
        }
        else {
          fireThumbMoved(new ThumbEvent(model, ThumbEvent.EventType.MOVE, delayedMove.getThumbIndex(), delayedMove.getStartingValue(), delayedMove.getAdjustingValue(), false));
          delayedMoveRef.set(new DelayedMove(System.currentTimeMillis(), thumbIndex, startingValue, adjustingValue));
        }
      }
      else {
        delayedMoveRef.set(new DelayedMove(System.currentTimeMillis(), thumbIndex, startingValue, adjustingValue));
      }
    }

    public synchronized void purge () {

      DelayedMove delayedMove;

      if ((delayedMove = delayedMoveRef.getAndSet(null)) != null) {
        fireThumbMoved(new ThumbEvent(model, ThumbEvent.EventType.MOVE, delayedMove.getThumbIndex(), delayedMove.getStartingValue(), delayedMove.getAdjustingValue(), false));
      }
    }

    public void finish ()
      throws InterruptedException {

      terminationLatch.countDown();
      exitLatch.await();
    }

    @Override
    public void run () {

      try {
        while (!terminationLatch.await(300, TimeUnit.MILLISECONDS)) {

          DelayedMove delayedMove;

          if ((delayedMove = delayedMoveRef.get()) != null) {
            if (System.currentTimeMillis() - delayedMove.getTimestamp() > 300) {
              purge();
            }
          }
        }
      }
      catch (InterruptedException interruptedException) {
        terminationLatch.countDown();
      }
      finally {
        exitLatch.countDown();
      }
    }

    @Override
    protected void finalize ()
      throws InterruptedException {

      finish();
    }
  }

  private class DelayedMove {

    private long timestamp;
    private int thumbIndex;
    private int startingValue;
    private int adjustingValue;

    private DelayedMove (long timestamp, int thumbIndex, int startingValue, int adjustingValue) {

      this.timestamp = timestamp;
      this.thumbIndex = thumbIndex;
      this.startingValue = startingValue;
      this.adjustingValue = adjustingValue;
    }

    public long getTimestamp () {

      return timestamp;
    }

    public void setTimestamp (long timestamp) {

      this.timestamp = timestamp;
    }

    public int getThumbIndex () {

      return thumbIndex;
    }

    public int getStartingValue () {

      return startingValue;
    }

    public int getAdjustingValue () {

      return adjustingValue;
    }

    public void setAdjustingValue (int adjustingValue) {

      this.adjustingValue = adjustingValue;
    }
  }
}
