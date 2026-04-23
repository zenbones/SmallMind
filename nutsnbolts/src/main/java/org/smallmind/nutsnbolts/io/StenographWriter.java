/*
 * Copyright (c) 2007 through 2026 David Berkman
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
package org.smallmind.nutsnbolts.io;

import java.io.IOException;
import java.io.Writer;
import org.smallmind.nutsnbolts.util.WeakEventListenerList;

/**
 * A {@link Writer} that accumulates written characters in an internal buffer and notifies registered
 * {@link StenographEventListener}s with the buffered text each time it is flushed or closed.
 */
public class StenographWriter extends Writer {

  private final WeakEventListenerList<StenographEventListener> listenerList = new WeakEventListenerList<StenographEventListener>();

  private final StringBuilder stenographBuilder = new StringBuilder();

  /**
   * Appends a slice of the given character array to the internal buffer.
   *
   * @param cbuf the source character array
   * @param off  the index of the first character to append
   * @param len  the number of characters to append
   * @throws IOException if an I/O error occurs (not thrown by this implementation)
   */
  @Override
  public synchronized void write (char[] cbuf, int off, int len)
    throws IOException {

    stenographBuilder.append(cbuf, off, len);
  }

  /**
   * Dispatches the accumulated buffer contents to all registered listeners and clears the buffer.
   *
   * @throws IOException if an I/O error occurs (not thrown by this implementation)
   */
  @Override
  public synchronized void flush ()
    throws IOException {

    fireFlush();
  }

  /**
   * Dispatches the accumulated buffer contents to all registered listeners, clears the buffer, and
   * leaves the writer in a closed state.
   *
   * @throws IOException if an I/O error occurs (not thrown by this implementation)
   */
  @Override
  public synchronized void close ()
    throws IOException {

    fireFlush();
  }

  /**
   * Constructs a {@link StenographEvent} from the current buffer, delivers it to every registered
   * listener, and then clears the buffer.
   */
  private void fireFlush () {

    StenographEvent event = new StenographEvent(this, stenographBuilder.toString());

    for (StenographEventListener listener : listenerList) {
      listener.flush(event);
    }

    stenographBuilder.delete(0, stenographBuilder.length());
  }

  /**
   * Registers a listener to be notified whenever this writer is flushed or closed.
   *
   * @param stenographEventListener the listener to register
   */
  public synchronized void addStenographListener (StenographEventListener stenographEventListener) {

    listenerList.addListener(stenographEventListener);
  }

  /**
   * Removes a previously registered flush listener.
   *
   * @param stenographEventListener the listener to remove
   */
  public synchronized void removeStenographListener (StenographEventListener stenographEventListener) {

    listenerList.removeListener(stenographEventListener);
  }
}
