/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
package org.smallmind.nutsnbolts.io;

import java.io.IOException;
import java.io.Writer;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

public class StenographWriter extends Writer {

  private final WeakEventListenerList<StenographEventListener> listenerList = new WeakEventListenerList<StenographEventListener>();

  private StringBuilder stenographBuilder = new StringBuilder();

  @Override
  public synchronized void write (char[] cbuf, int off, int len)
    throws IOException {

    stenographBuilder.append(cbuf, off, len);
  }

  @Override
  public synchronized void flush ()
    throws IOException {

    fireFlush();
  }

  @Override
  public synchronized void close ()
    throws IOException {

    fireFlush();
  }

  private void fireFlush () {

    StenographEvent event = new StenographEvent(this, stenographBuilder.toString());

    for (StenographEventListener listener : listenerList) {
      listener.flush(event);
    }

    stenographBuilder.delete(0, stenographBuilder.length());
  }

  public synchronized void addStenographListener (StenographEventListener stenographEventListener) {

    listenerList.addListener(stenographEventListener);
  }

  public synchronized void removeStenographListener (StenographEventListener stenographEventListener) {

    listenerList.removeListener(stenographEventListener);
  }
}
