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
package org.smallmind.nutsnbolts.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe list of event listeners held as weak references, allowing listeners to be garbage-collected when no other strong references remain; a shared background scrubber thread removes cleared entries automatically.
 *
 * @param <E> listener type, must extend {@link EventListener}
 */
public class WeakEventListenerList<E extends EventListener> implements Iterable<E> {

  private static final Scrubber SCRUBBER;
  private final LinkedList<WeakReference<E>> referenceList;

  static {
    Thread scrubberThread;

    SCRUBBER = new Scrubber();
    scrubberThread = new Thread(SCRUBBER);
    scrubberThread.setDaemon(true);
    scrubberThread.start();
  }

  /**
   * Constructs an empty weak listener list.
   */
  public WeakEventListenerList () {

    referenceList = new LinkedList<>();
  }

  /**
   * Returns an iterator over all listeners that are still reachable (have not been garbage-collected).
   *
   * @return iterator over currently live listeners
   */
  public Iterator<E> getListeners () {

    LinkedList<E> listenerList;
    Iterator<WeakReference<E>> listenerReferenceIter;
    E eventListener;

    listenerList = new LinkedList<E>();

    synchronized (referenceList) {
      listenerReferenceIter = referenceList.iterator();
      while (listenerReferenceIter.hasNext()) {
        if ((eventListener = (listenerReferenceIter.next()).get()) != null) {
          listenerList.add(eventListener);
        }
      }
    }

    return listenerList.iterator();
  }

  /**
   * Registers a listener via a weak reference so it may be garbage-collected if no other strong references exist.
   *
   * @param eventListener the listener to register
   */
  public void addListener (E eventListener) {

    synchronized (referenceList) {
      referenceList.add(SCRUBBER.createReference(this, eventListener));
    }
  }

  /**
   * Removes all registered listeners from this list.
   */
  public void removeAllListeners () {

    synchronized (referenceList) {
      referenceList.clear();
    }
  }

  /**
   * Removes the specified listener from this list if it is present.
   *
   * @param eventListener the listener instance to remove
   */
  public void removeListener (E eventListener) {

    Iterator<WeakReference<E>> referenceIter;

    synchronized (referenceList) {
      referenceIter = referenceList.iterator();
      while (referenceIter.hasNext()) {
        if ((referenceIter.next()).get().equals(eventListener)) {
          referenceIter.remove();
        }
      }
    }
  }

  private void removeListener (Reference<E> reference) {

    synchronized (referenceList) {
      referenceList.remove(reference);
    }
  }

  /**
   * Returns an iterator over all live listeners; equivalent to {@link #getListeners()}.
   *
   * @return iterator over currently live listeners
   */
  public Iterator<E> iterator () {

    return getListeners();
  }

  /**
   * Shared daemon responsible for draining the reference queue and removing cleared listener references from their parent lists.
   */
  public static class Scrubber implements Runnable {

    private final CountDownLatch exitLatch;
    private final ReferenceQueue<EventListener> referenceQueue;
    private final HashMap<WeakReference<? extends EventListener>, WeakEventListenerList> parentMap;
    private final AtomicBoolean finished = new AtomicBoolean(false);

    /**
     * Constructs the scrubber, initializing the reference queue and parent-list registry.
     */
    public Scrubber () {

      referenceQueue = new ReferenceQueue<>();
      parentMap = new HashMap<>();

      exitLatch = new CountDownLatch(1);
    }

    /**
     * Creates a weak reference for the listener, registers it against the parent list for later cleanup, and returns the reference.
     *
     * @param parent        the owning {@link WeakEventListenerList}
     * @param eventListener the listener to wrap
     * @param <E>           the listener type
     * @return a weak reference to the listener, linked to this scrubber's reference queue
     */
    public <E extends EventListener> WeakReference<E> createReference (WeakEventListenerList parent, E eventListener) {

      WeakReference<E> reference;

      reference = new WeakReference<>(eventListener, referenceQueue);
      parentMap.put(reference, parent);

      return reference;
    }

    /**
     * Signals the scrubber to stop and blocks until it has exited.
     *
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    public void finish ()
      throws InterruptedException {

      finished.set(true);
      exitLatch.await();
    }

    /**
     * Drains the reference queue in a loop, removing cleared listener references from their parent lists, until {@link #finish()} is called or the thread is interrupted.
     */
    public void run () {

      Reference<? extends EventListener> reference;

      while (!finished.get()) {
        try {
          if ((reference = referenceQueue.remove(1000)) != null) {
            parentMap.remove(reference).removeListener(reference);
          }
        } catch (InterruptedException interruptedException) {
          finished.set(true);
        }
      }

      exitLatch.countDown();
    }
  }
}
