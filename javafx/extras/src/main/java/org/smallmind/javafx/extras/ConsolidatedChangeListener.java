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
package org.smallmind.javafx.extras;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import org.smallmind.scribe.pen.LoggerManager;

public abstract class ConsolidatedChangeListener<T> implements ChangeListener<T> {

  private final AtomicReference<T> currentValue = new AtomicReference<>();
  private final AtomicLong startTime = new AtomicLong(0);
  private final long consolidationTimeMillis;
  private boolean initialCall = true;

  public ConsolidatedChangeListener (long consolidationTimeMillis) {

    this.consolidationTimeMillis = consolidationTimeMillis;
  }

  public abstract void consolidatedChange (ObservableValue<? extends T> observableValue, T t1, T t2);

  @Override
  public final synchronized void changed (ObservableValue<? extends T> observableValue, T t1, T t2) {

    if (initialCall) {

      T initialValue;

      initialCall = false;
      initialValue = t1;
      currentValue.set(t2);

      startTime.set(System.currentTimeMillis());

      try {

        long currentlyPassed;

        while ((currentlyPassed = System.currentTimeMillis() - startTime.get()) < consolidationTimeMillis) {
          wait(consolidationTimeMillis - currentlyPassed);
        }
      }
      catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(ConsolidatedChangeListener.class).error(interruptedException);
      }

      consolidatedChange(observableValue, initialValue, currentValue.get());
      initialCall = true;
    }
    else {
      currentValue.set(t2);
      startTime.set(System.currentTimeMillis());
    }
  }
}
