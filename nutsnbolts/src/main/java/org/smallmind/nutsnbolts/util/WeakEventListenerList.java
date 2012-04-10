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

      referenceList = new LinkedList<WeakReference<E>>();
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

      private CountDownLatch exitLatch;
      private ReferenceQueue<EventListener> referenceQueue;
      private HashMap<WeakReference<? extends EventListener>, WeakEventListenerList> parentMap;
      private AtomicBoolean finished = new AtomicBoolean(false);

      public Scrubber () {

         referenceQueue = new ReferenceQueue<EventListener>();
         parentMap = new HashMap<WeakReference<? extends EventListener>, WeakEventListenerList>();

         exitLatch = new CountDownLatch(1);
      }

      public <E extends EventListener> WeakReference<E> createReference (WeakEventListenerList parent, E eventListener) {

         WeakReference<E> reference;

         reference = new WeakReference<E>(eventListener, referenceQueue);
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
            }
            catch (InterruptedException i) {
            }
         }

         exitLatch.countDown();
      }
   }
}