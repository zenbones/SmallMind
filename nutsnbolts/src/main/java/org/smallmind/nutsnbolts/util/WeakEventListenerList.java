/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

  public WeakEventListenerList () {

    referenceList = new LinkedList<>();
  }

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

  public void addListener (E eventListener) {

    synchronized (referenceList) {
      referenceList.add(SCRUBBER.createReference(this, eventListener));
    }
  }

  public void removeAllListeners () {

    synchronized (referenceList) {
      referenceList.clear();
    }
  }

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

  public Iterator<E> iterator () {

    return getListeners();
  }

  public static class Scrubber implements Runnable {

    private final CountDownLatch exitLatch;
    private final ReferenceQueue<EventListener> referenceQueue;
    private final HashMap<WeakReference<? extends EventListener>, WeakEventListenerList> parentMap;
    private final AtomicBoolean finished = new AtomicBoolean(false);

    public Scrubber () {

      referenceQueue = new ReferenceQueue<>();
      parentMap = new HashMap<>();

      exitLatch = new CountDownLatch(1);
    }

    public <E extends EventListener> WeakReference<E> createReference (WeakEventListenerList parent, E eventListener) {

      WeakReference<E> reference;

      reference = new WeakReference<>(eventListener, referenceQueue);
      parentMap.put(reference, parent);

      return reference;
    }

    public void finish ()
      throws InterruptedException {

      finished.set(true);
      exitLatch.await();
    }

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