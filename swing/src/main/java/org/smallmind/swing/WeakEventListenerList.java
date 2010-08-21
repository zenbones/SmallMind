package org.smallmind.swing;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class WeakEventListenerList<E extends EventListener> implements Iterable<E> {

   private static Scrubber SCRUBBER;

   private final LinkedList<WeakReference<E>> referenceList;

   static {
      Thread scrubberThread;

      SCRUBBER = new Scrubber();
      scrubberThread = new Thread(SCRUBBER);
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
            if ((referenceIter.next()).get() == eventListener) {
               referenceIter.remove();
               break;
            }
         }
      }
   }

   private void removeListener (Reference<? extends EventListener> reference) {

      synchronized (referenceList) {
         referenceList.remove(reference);
      }
   }

   public Iterator<E> iterator () {

      return getListeners();
   }

   public static class Scrubber implements Runnable {

      private Thread runnableThread;
      private ReferenceQueue<EventListener> referenceQueue;
      private HashMap<WeakReference<? extends EventListener>, WeakEventListenerList> parentMap;
      private boolean finished = false;
      private boolean exited = false;

      public Scrubber () {

         referenceQueue = new ReferenceQueue<EventListener>();
         parentMap = new HashMap<WeakReference<? extends EventListener>, WeakEventListenerList>();
      }

      public <E extends EventListener> WeakReference<E> createReference (WeakEventListenerList parent, E eventListener) {

         WeakReference<E> reference;

         reference = new WeakReference<E>(eventListener, referenceQueue);
         parentMap.put(reference, parent);

         return reference;
      }

      public void finish () {

         finished = true;

         while (!exited) {
            runnableThread.interrupt();

            try {
               Thread.sleep(100);
            }
            catch (InterruptedException i) {
            }
         }
      }

      public void run () {

         Reference<? extends EventListener> reference;

         runnableThread = Thread.currentThread();

         while (!finished) {
            try {
               if ((reference = referenceQueue.remove(1000)) != null) {
                  parentMap.remove(reference).removeListener(reference);
               }
            }
            catch (InterruptedException i) {
            }
         }

         exited = true;
      }

      public void finalize () {

         finish();
      }
   }
}
